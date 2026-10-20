package mz.plataformaacademica.administracao;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.plataformaacademica.comum.ConflitoException;
import mz.plataformaacademica.comum.NaoEncontradoException;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@Service
public class ServicoDeTiposDeInstituicao {

    public record TipoVista(
        UUID id, String nome, String descricao, String estado, UUID propostoPor, UUID decididoPor) {
    }

    private static final RowMapper<TipoVista> MAPEADOR = (rs, linha) -> new TipoVista(
        rs.getObject("id", UUID.class),
        rs.getString("nome"),
        rs.getString("descricao"),
        rs.getString("estado"),
        rs.getObject("proposto_por", UUID.class),
        rs.getObject("decidido_por", UUID.class));

    private static final String LISTAR =
        "SELECT id, nome, descricao, estado, proposto_por, decidido_por FROM tipo_instituicao ORDER BY lower(nome)";

    private static final String OBTER =
        "SELECT id, nome, descricao, estado, proposto_por, decidido_por FROM tipo_instituicao WHERE id = ?";

    private static final String PROPOR =
        "INSERT INTO tipo_instituicao (nome, descricao, proposto_por) VALUES (?, ?, ?) ON CONFLICT DO NOTHING RETURNING id";

    private static final String BLOQUEAR_ESTADO =
        "SELECT estado FROM tipo_instituicao WHERE id = ? FOR UPDATE";

    private static final String APROVAR =
        "UPDATE tipo_instituicao SET estado = 'APROVADA', data_aprovacao = now(), decidido_por = ? WHERE id = ?";

    private static final String REJEITAR =
        "UPDATE tipo_instituicao SET estado = 'REJEITADA', decidido_por = ? WHERE id = ?";

    private final JdbcTemplate jdbc;

    public ServicoDeTiposDeInstituicao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<TipoVista> listar() {
        return jdbc.query(LISTAR, MAPEADOR);
    }

    @Transactional(readOnly = true)
    public TipoVista obter(UUID id) {
        List<TipoVista> encontrados = jdbc.query(OBTER, MAPEADOR, id);
        if (encontrados.isEmpty()) {
            throw new NaoEncontradoException("Tipo de instituição não encontrado");
        }
        return encontrados.get(0);
    }

    @Transactional
    public TipoVista propor(String nome, String descricao, UUID proponente) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome é obrigatório");
        }
        String nomeLimpo = nome.strip();
        if (nomeLimpo.length() > 100) {
            throw new RegraDeNegocioException("O nome é demasiado longo");
        }
        String descricaoLimpa = descricao == null || descricao.isBlank() ? null : descricao.strip();
        List<UUID> criados = jdbc.queryForList(PROPOR, UUID.class, nomeLimpo, descricaoLimpa, proponente);
        if (criados.isEmpty()) {
            throw new ConflitoException("Já existe um tipo de instituição com esse nome");
        }
        return obter(criados.get(0));
    }

    @Transactional
    public TipoVista aprovar(UUID id, UUID decisor) {
        verificarPendente(id);
        jdbc.update(APROVAR, decisor, id);
        return obter(id);
    }

    @Transactional
    public TipoVista rejeitar(UUID id, UUID decisor) {
        verificarPendente(id);
        jdbc.update(REJEITAR, decisor, id);
        return obter(id);
    }

    private void verificarPendente(UUID id) {
        List<String> estados = jdbc.queryForList(BLOQUEAR_ESTADO, String.class, id);
        if (estados.isEmpty()) {
            throw new NaoEncontradoException("Tipo de instituição não encontrado");
        }
        if (!"PENDENTE".equals(estados.get(0))) {
            throw new RegraDeNegocioException(
                "O tipo de instituição já foi decidido (estado actual: " + estados.get(0) + ")");
        }
    }
}

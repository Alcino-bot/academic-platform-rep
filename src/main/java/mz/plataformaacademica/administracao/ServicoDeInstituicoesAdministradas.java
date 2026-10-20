package mz.plataformaacademica.administracao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.plataformaacademica.comum.NaoEncontradoException;
import mz.plataformaacademica.comum.RegraDeNegocioException;
import mz.plataformaacademica.instituicao.EstadoInstituicao;
import mz.plataformaacademica.instituicao.Instituicao;
import mz.plataformaacademica.instituicao.InstituicaoServico;

@Service
public class ServicoDeInstituicoesAdministradas {

    private record Modelo(UUID tipoInstituicaoId, String estado) {
    }

    private record TipoDoModelo(UUID id, String nome, int nivel, UUID paiId) {
    }

    private static final String OBTER_MODELO =
        "SELECT tipo_instituicao_id, estado FROM modelo_organizacional WHERE id = ?";

    private static final String TIPOS_DO_MODELO =
        "SELECT id, nome, nivel, tipo_pai_id FROM modelo_tipo_unidade WHERE modelo_id = ? ORDER BY nivel, lower(nome)";

    private static final String COPIAR_TIPO =
        "INSERT INTO tipo_unidade (instituicao_id, nome, nivel, tipo_pai_id, modelo_tipo_origem_id) VALUES (?, ?, ?, ?, ?) RETURNING id";

    private final InstituicaoServico instituicoes;
    private final JdbcTemplate jdbc;

    public ServicoDeInstituicoesAdministradas(InstituicaoServico instituicoes, JdbcTemplate jdbc) {
        this.instituicoes = instituicoes;
        this.jdbc = jdbc;
    }

    @Transactional
    public Instituicao criarActivaAPartirDoModelo(String nome, String sigla, UUID modeloId) {
        if (modeloId == null) {
            throw new RegraDeNegocioException("O modelo organizacional é obrigatório");
        }
        List<Modelo> modelos = jdbc.query(
            OBTER_MODELO,
            (rs, linha) -> new Modelo(rs.getObject("tipo_instituicao_id", UUID.class), rs.getString("estado")),
            modeloId);
        if (modelos.isEmpty()) {
            throw new NaoEncontradoException("Modelo organizacional não encontrado");
        }
        Modelo modelo = modelos.get(0);
        if (!"ACTIVO".equals(modelo.estado())) {
            throw new RegraDeNegocioException("O modelo organizacional não está activo");
        }
        Instituicao instituicao = instituicoes.criar(nome, sigla, modelo.tipoInstituicaoId(), modeloId);
        copiarTiposDeUnidade(instituicao.getId(), modeloId);
        instituicoes.mudarEstado(instituicao.getId(), EstadoInstituicao.ACTIVA);
        return instituicao;
    }

    private void copiarTiposDeUnidade(UUID instituicaoId, UUID modeloId) {
        List<TipoDoModelo> tipos = jdbc.query(
            TIPOS_DO_MODELO,
            (rs, linha) -> new TipoDoModelo(
                rs.getObject("id", UUID.class),
                rs.getString("nome"),
                rs.getInt("nivel"),
                rs.getObject("tipo_pai_id", UUID.class)),
            modeloId);
        Map<UUID, UUID> copias = new HashMap<>();
        for (TipoDoModelo tipo : tipos) {
            UUID paiCopiado = tipo.paiId() == null ? null : copias.get(tipo.paiId());
            if (tipo.paiId() != null && paiCopiado == null) {
                throw new RegraDeNegocioException(
                    "O modelo organizacional é inconsistente: o tipo de unidade '" + tipo.nome()
                        + "' não tem um tipo pai de nível anterior");
            }
            List<UUID> criados = jdbc.queryForList(
                COPIAR_TIPO, UUID.class, instituicaoId, tipo.nome(), tipo.nivel(), paiCopiado, tipo.id());
            copias.put(tipo.id(), criados.get(0));
        }
    }
}

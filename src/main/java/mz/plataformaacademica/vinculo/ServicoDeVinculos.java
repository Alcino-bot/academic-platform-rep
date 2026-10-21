package mz.plataformaacademica.vinculo;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.plataformaacademica.comum.ConflitoException;
import mz.plataformaacademica.comum.NaoEncontradoException;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@Service
public class ServicoDeVinculos {

    public record VinculoVista(
        UUID id,
        UUID utilizadorId,
        String nome,
        String email,
        UUID instituicaoId,
        String instituicaoNome,
        String tipoVinculo,
        String estado,
        String motivoRejeicao,
        List<String> papeis) {
    }

    private record Pendente(UUID utilizadorId, String tipoVinculo, String estado) {
    }

    static final Set<String> TIPOS = Set.of("ESTUDANTE", "DOCENTE", "FUNCIONARIO", "OUTRO");
    static final Set<String> ESTADOS = Set.of("PENDENTE", "ACTIVO", "SUSPENSO", "TERMINADO", "REJEITADO");
    static final Set<String> PAPEIS_ATRIBUIVEIS = Set.of("DOCENTE", "ESTUDANTE", "REVISOR_BIBLIOTECA");

    private static final RowMapper<VinculoVista> MAPEADOR = (rs, linha) -> {
        String papeis = rs.getString("papeis");
        return new VinculoVista(
            rs.getObject("id", UUID.class),
            rs.getObject("utilizador_id", UUID.class),
            rs.getString("nome_completo"),
            rs.getString("email"),
            rs.getObject("instituicao_id", UUID.class),
            rs.getString("instituicao_nome"),
            rs.getString("tipo_vinculo"),
            rs.getString("estado"),
            rs.getString("motivo_rejeicao"),
            papeis == null || papeis.isEmpty() ? List.of() : Arrays.asList(papeis.split(",")));
    };

    private static final String BASE =
        "SELECT v.id, v.utilizador_id, u.nome_completo, u.email, v.instituicao_id, i.nome AS instituicao_nome, v.tipo_vinculo, v.estado, v.motivo_rejeicao, "
            + "COALESCE((SELECT string_agg(p.codigo, ',' ORDER BY p.codigo) FROM vinculo_papel vp JOIN papel p ON p.id = vp.papel_id WHERE vp.vinculo_id = v.id AND vp.instituicao_id = v.instituicao_id AND vp.estado = 'ACTIVO'), '') AS papeis "
            + "FROM vinculo_institucional v JOIN utilizador u ON u.id = v.utilizador_id JOIN instituicao i ON i.id = v.instituicao_id ";

    private static final String MEUS = BASE + "WHERE v.utilizador_id = ? ORDER BY v.data_criacao DESC, v.id";

    private static final String DA_INSTITUICAO =
        BASE + "WHERE v.instituicao_id = ? ORDER BY v.data_criacao DESC, v.id";

    private static final String DA_INSTITUICAO_POR_ESTADO =
        BASE + "WHERE v.instituicao_id = ? AND v.estado = ? ORDER BY v.data_criacao DESC, v.id";

    private static final String OBTER = BASE + "WHERE v.id = ? AND v.instituicao_id = ?";

    private static final String ESTADO_DA_INSTITUICAO = "SELECT estado FROM instituicao WHERE id = ?";

    private static final String PEDIR =
        "INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo) VALUES (?, ?, ?) "
            + "ON CONFLICT (utilizador_id, instituicao_id, tipo_vinculo) DO UPDATE SET estado = 'PENDENTE', data_inicio = CURRENT_DATE, data_fim = NULL, decidido_por = NULL, data_decisao = NULL, motivo_rejeicao = NULL "
            + "WHERE vinculo_institucional.estado IN ('REJEITADO', 'TERMINADO') RETURNING id";

    private static final String BLOQUEAR_PEDIDO =
        "SELECT utilizador_id, tipo_vinculo, estado FROM vinculo_institucional WHERE id = ? AND instituicao_id = ? FOR UPDATE";

    private static final String APROVAR =
        "UPDATE vinculo_institucional SET estado = 'ACTIVO', data_inicio = CURRENT_DATE, decidido_por = ?, data_decisao = now() WHERE id = ? AND instituicao_id = ?";

    private static final String REJEITAR =
        "UPDATE vinculo_institucional SET estado = 'REJEITADO', decidido_por = ?, data_decisao = now(), motivo_rejeicao = ? WHERE id = ? AND instituicao_id = ?";

    static final String ATRIBUIR_PAPEL =
        "INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id, atribuido_por) "
            + "SELECT CAST(? AS uuid), CAST(? AS uuid), id, CAST(? AS uuid) FROM papel "
            + "WHERE codigo = ? AND ambito = 'INSTITUCIONAL' AND estado = 'ACTIVO' ON CONFLICT DO NOTHING";

    private static final String REVOGAR_PAPEL =
        "UPDATE vinculo_papel SET estado = 'REVOGADO', data_revogacao = now() "
            + "WHERE vinculo_id = ? AND instituicao_id = ? AND estado = 'ACTIVO' "
            + "AND papel_id = (SELECT id FROM papel WHERE codigo = ?)";

    private final JdbcTemplate jdbc;

    public ServicoDeVinculos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<VinculoVista> meus(UUID utilizadorId) {
        return jdbc.query(MEUS, MAPEADOR, utilizadorId);
    }

    @Transactional(readOnly = true)
    public List<VinculoVista> daInstituicao(UUID instituicaoId, String estado) {
        if (estado == null || estado.isBlank()) {
            return jdbc.query(DA_INSTITUICAO, MAPEADOR, instituicaoId);
        }
        if (!ESTADOS.contains(estado)) {
            throw new RegraDeNegocioException("Estado de vínculo inválido");
        }
        return jdbc.query(DA_INSTITUICAO_POR_ESTADO, MAPEADOR, instituicaoId, estado);
    }

    @Transactional
    public VinculoVista pedir(UUID utilizadorId, UUID instituicaoId, String tipoVinculo) {
        if (tipoVinculo == null || !TIPOS.contains(tipoVinculo)) {
            throw new RegraDeNegocioException("Tipo de vínculo inválido");
        }
        List<String> estados = jdbc.queryForList(ESTADO_DA_INSTITUICAO, String.class, instituicaoId);
        if (estados.isEmpty()) {
            throw new NaoEncontradoException("Instituição não encontrada");
        }
        if (!"ACTIVA".equals(estados.get(0))) {
            throw new RegraDeNegocioException("A instituição não está activa");
        }
        List<UUID> criados = jdbc.queryForList(PEDIR, UUID.class, utilizadorId, instituicaoId, tipoVinculo);
        if (criados.isEmpty()) {
            throw new ConflitoException("Já tem um vínculo ou um pedido pendente deste tipo nesta instituição");
        }
        return obter(criados.get(0), instituicaoId);
    }

    @Transactional
    public VinculoVista aprovar(UUID instituicaoId, UUID vinculoId, UUID decisor) {
        Pendente pedido = bloquearPendente(instituicaoId, vinculoId, decisor);
        jdbc.update(APROVAR, decisor, vinculoId, instituicaoId);
        if ("ESTUDANTE".equals(pedido.tipoVinculo()) || "DOCENTE".equals(pedido.tipoVinculo())) {
            jdbc.update(ATRIBUIR_PAPEL, vinculoId, instituicaoId, decisor, pedido.tipoVinculo());
        }
        return obter(vinculoId, instituicaoId);
    }

    @Transactional
    public VinculoVista rejeitar(UUID instituicaoId, UUID vinculoId, UUID decisor, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new RegraDeNegocioException("O motivo da rejeição é obrigatório");
        }
        bloquearPendente(instituicaoId, vinculoId, decisor);
        jdbc.update(REJEITAR, decisor, motivo.strip(), vinculoId, instituicaoId);
        return obter(vinculoId, instituicaoId);
    }

    @Transactional
    public VinculoVista atribuirPapel(UUID instituicaoId, UUID vinculoId, String papel, UUID atribuidoPor) {
        validarPapel(papel);
        List<Pendente> encontrados = jdbc.query(
            BLOQUEAR_PEDIDO,
            (rs, linha) -> new Pendente(
                rs.getObject("utilizador_id", UUID.class), rs.getString("tipo_vinculo"), rs.getString("estado")),
            vinculoId, instituicaoId);
        if (encontrados.isEmpty()) {
            throw new NaoEncontradoException("Vínculo não encontrado");
        }
        Pendente vinculo = encontrados.get(0);
        if (!"ACTIVO".equals(vinculo.estado())) {
            throw new RegraDeNegocioException("O vínculo não está activo");
        }
        if (("ESTUDANTE".equals(papel) || "DOCENTE".equals(papel)) && !papel.equals(vinculo.tipoVinculo())) {
            throw new RegraDeNegocioException("O papel " + papel + " só pode ser atribuído a um vínculo do tipo " + papel);
        }
        jdbc.update(ATRIBUIR_PAPEL, vinculoId, instituicaoId, atribuidoPor, papel);
        return obter(vinculoId, instituicaoId);
    }

    @Transactional
    public VinculoVista revogarPapel(UUID instituicaoId, UUID vinculoId, String papel) {
        validarPapel(papel);
        obter(vinculoId, instituicaoId);
        int revogados = jdbc.update(REVOGAR_PAPEL, vinculoId, instituicaoId, papel);
        if (revogados == 0) {
            throw new NaoEncontradoException("O vínculo não tem esse papel activo");
        }
        return obter(vinculoId, instituicaoId);
    }

    @Transactional(readOnly = true)
    public VinculoVista obter(UUID vinculoId, UUID instituicaoId) {
        List<VinculoVista> encontrados = jdbc.query(OBTER, MAPEADOR, vinculoId, instituicaoId);
        if (encontrados.isEmpty()) {
            throw new NaoEncontradoException("Vínculo não encontrado");
        }
        return encontrados.get(0);
    }

    private Pendente bloquearPendente(UUID instituicaoId, UUID vinculoId, UUID decisor) {
        List<Pendente> encontrados = jdbc.query(
            BLOQUEAR_PEDIDO,
            (rs, linha) -> new Pendente(
                rs.getObject("utilizador_id", UUID.class), rs.getString("tipo_vinculo"), rs.getString("estado")),
            vinculoId, instituicaoId);
        if (encontrados.isEmpty()) {
            throw new NaoEncontradoException("Pedido de vínculo não encontrado");
        }
        Pendente pedido = encontrados.get(0);
        if (pedido.utilizadorId().equals(decisor)) {
            throw new RegraDeNegocioException("Não pode decidir o seu próprio pedido");
        }
        if (!"PENDENTE".equals(pedido.estado())) {
            throw new RegraDeNegocioException("O pedido já foi decidido (estado actual: " + pedido.estado() + ")");
        }
        return pedido;
    }

    private static void validarPapel(String papel) {
        if (papel == null || !PAPEIS_ATRIBUIVEIS.contains(papel)) {
            throw new RegraDeNegocioException(
                "Papel não permitido: só se atribuem DOCENTE, ESTUDANTE e REVISOR_BIBLIOTECA");
        }
    }
}

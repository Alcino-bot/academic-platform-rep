package mz.plataformaacademica.administracao;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.plataformaacademica.comum.NaoEncontradoException;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@Service
public class ServicoDeAdministradoresDeInstituicao {

    public record Designacao(UUID vinculoId, boolean jaEraAdministrador) {
    }

    private static final String ESTADO_DA_INSTITUICAO = "SELECT estado FROM instituicao WHERE id = ?";

    private static final String CONTA_ACTIVA =
        "SELECT id FROM utilizador WHERE lower(btrim(email)) = lower(btrim(?)) AND estado = 'ACTIVO'";

    private static final String VINCULO_ACTIVO =
        "SELECT id FROM vinculo_institucional WHERE utilizador_id = ? AND instituicao_id = ? AND estado = 'ACTIVO' ORDER BY data_criacao, id LIMIT 1";

    private static final String CRIAR_VINCULO =
        "INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo, estado, decidido_por, data_decisao) "
            + "VALUES (?, ?, 'FUNCIONARIO', 'ACTIVO', ?, now()) "
            + "ON CONFLICT (utilizador_id, instituicao_id, tipo_vinculo) DO UPDATE SET estado = 'ACTIVO', data_inicio = CURRENT_DATE, data_fim = NULL, decidido_por = EXCLUDED.decidido_por, data_decisao = now(), motivo_rejeicao = NULL "
            + "WHERE vinculo_institucional.estado IN ('PENDENTE', 'REJEITADO', 'TERMINADO') RETURNING id";

    private static final String ATRIBUIR_ADMINISTRADOR =
        "INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id, atribuido_por) "
            + "SELECT CAST(? AS uuid), CAST(? AS uuid), id, CAST(? AS uuid) FROM papel "
            + "WHERE codigo = 'ADMIN_INSTITUICAO' AND ambito = 'INSTITUCIONAL' AND estado = 'ACTIVO' ON CONFLICT DO NOTHING";

    private final JdbcTemplate jdbc;

    public ServicoDeAdministradoresDeInstituicao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Designacao designar(UUID instituicaoId, String email, UUID designadoPor) {
        if (email == null || email.isBlank()) {
            throw new RegraDeNegocioException("O email é obrigatório");
        }
        List<String> estados = jdbc.queryForList(ESTADO_DA_INSTITUICAO, String.class, instituicaoId);
        if (estados.isEmpty()) {
            throw new NaoEncontradoException("Instituição não encontrada");
        }
        if (!"ACTIVA".equals(estados.get(0))) {
            throw new RegraDeNegocioException("A instituição não está activa");
        }
        List<UUID> contas = jdbc.queryForList(CONTA_ACTIVA, UUID.class, email);
        if (contas.isEmpty()) {
            throw new NaoEncontradoException("Não existe uma conta activa com esse email");
        }
        UUID vinculoId = vinculoActivoOuNovo(contas.get(0), instituicaoId, designadoPor);
        int atribuidos = jdbc.update(ATRIBUIR_ADMINISTRADOR, vinculoId, instituicaoId, designadoPor);
        return new Designacao(vinculoId, atribuidos == 0);
    }

    private UUID vinculoActivoOuNovo(UUID utilizadorId, UUID instituicaoId, UUID designadoPor) {
        List<UUID> activos = jdbc.queryForList(VINCULO_ACTIVO, UUID.class, utilizadorId, instituicaoId);
        if (!activos.isEmpty()) {
            return activos.get(0);
        }
        List<UUID> criados = jdbc.queryForList(CRIAR_VINCULO, UUID.class, utilizadorId, instituicaoId, designadoPor);
        if (criados.isEmpty()) {
            throw new RegraDeNegocioException("O vínculo da pessoa nesta instituição está suspenso");
        }
        return criados.get(0);
    }
}

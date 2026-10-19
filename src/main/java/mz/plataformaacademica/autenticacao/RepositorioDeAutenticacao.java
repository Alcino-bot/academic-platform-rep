package mz.plataformaacademica.autenticacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDeAutenticacao {

    private static final String PROCURAR_POR_EMAIL =
        "SELECT id, email, nome_completo, palavra_passe_hash, estado, (bloqueado_ate IS NOT NULL AND bloqueado_ate > now()) AS bloqueada FROM utilizador WHERE lower(btrim(email)) = lower(btrim(?))";

    private static final String REGISTAR_FALHA =
        "SELECT registar_falha_login(?, ?, make_interval(secs => ?))";

    private static final String REGISTAR_LOGIN_VALIDO =
        "UPDATE utilizador SET tentativas_falhadas = 0, bloqueado_ate = NULL, ultimo_acesso = now() WHERE id = ?";

    private static final String CONTA_ACTIVA =
        "SELECT EXISTS (SELECT 1 FROM utilizador WHERE id = ? AND estado = 'ACTIVO')";

    private static final String INSTITUICOES_DISPONIVEIS =
        "SELECT i.id, i.nome, i.sigla FROM instituicao i WHERE i.estado = 'ACTIVA' AND EXISTS (SELECT 1 FROM vinculo_institucional v WHERE v.instituicao_id = i.id AND v.utilizador_id = ? AND v.estado = 'ACTIVO') ORDER BY i.nome";

    private static final String PODE_ACTUAR =
        "SELECT EXISTS (SELECT 1 FROM instituicao i JOIN vinculo_institucional v ON v.instituicao_id = i.id WHERE i.id = ? AND i.estado = 'ACTIVA' AND v.utilizador_id = ? AND v.estado = 'ACTIVO')";

    private static final String AUTORIDADES_GLOBAIS =
        "SELECT 'PAPEL_GLOBAL_' || p.codigo FROM atribuicao_global a JOIN papel p ON p.id = a.papel_id WHERE a.utilizador_id = ? AND a.estado = 'ACTIVA' AND p.estado = 'ACTIVO'";

    private static final String AUTORIDADES_DA_INSTITUICAO =
        "SELECT 'PAPEL_' || p.codigo FROM vinculo_papel vp JOIN vinculo_institucional v ON v.id = vp.vinculo_id AND v.instituicao_id = vp.instituicao_id JOIN papel p ON p.id = vp.papel_id WHERE v.utilizador_id = ? AND vp.instituicao_id = ? AND vp.estado = 'ACTIVO' AND v.estado = 'ACTIVO' AND p.estado = 'ACTIVO' UNION SELECT 'VINCULO_' || v.tipo_vinculo FROM vinculo_institucional v WHERE v.utilizador_id = ? AND v.instituicao_id = ? AND v.estado = 'ACTIVO'";

    private final JdbcTemplate jdbc;

    public RepositorioDeAutenticacao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<RegistoUtilizador> procurarPorEmail(String email) {
        List<RegistoUtilizador> encontrados = jdbc.query(
            PROCURAR_POR_EMAIL,
            (rs, linha) -> new RegistoUtilizador(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("nome_completo"),
                rs.getString("palavra_passe_hash"),
                rs.getString("estado"),
                rs.getBoolean("bloqueada")),
            email);
        return encontrados.stream().findFirst();
    }

    public void registarFalha(UUID utilizadorId, int maximoTentativas, long duracaoSegundos) {
        jdbc.queryForObject(REGISTAR_FALHA, Object.class, utilizadorId, maximoTentativas, (double) duracaoSegundos);
    }

    public void registarLoginValido(UUID utilizadorId) {
        jdbc.update(REGISTAR_LOGIN_VALIDO, utilizadorId);
    }

    public boolean contaActiva(UUID utilizadorId) {
        Boolean activa = jdbc.queryForObject(CONTA_ACTIVA, Boolean.class, utilizadorId);
        return Boolean.TRUE.equals(activa);
    }

    public List<InstituicaoDisponivel> instituicoesDisponiveis(UUID utilizadorId) {
        return jdbc.query(
            INSTITUICOES_DISPONIVEIS,
            (rs, linha) -> new InstituicaoDisponivel(
                rs.getObject("id", UUID.class),
                rs.getString("nome"),
                rs.getString("sigla")),
            utilizadorId);
    }

    public boolean podeActuarNaInstituicao(UUID utilizadorId, UUID instituicaoId) {
        Boolean pode = jdbc.queryForObject(PODE_ACTUAR, Boolean.class, instituicaoId, utilizadorId);
        return Boolean.TRUE.equals(pode);
    }

    public List<String> autoridadesGlobais(UUID utilizadorId) {
        return jdbc.queryForList(AUTORIDADES_GLOBAIS, String.class, utilizadorId);
    }

    public List<String> autoridadesDaInstituicao(UUID utilizadorId, UUID instituicaoId) {
        return jdbc.queryForList(
            AUTORIDADES_DA_INSTITUICAO, String.class, utilizadorId, instituicaoId, utilizadorId, instituicaoId);
    }
}

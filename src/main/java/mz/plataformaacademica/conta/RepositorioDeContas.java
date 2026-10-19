package mz.plataformaacademica.conta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDeContas {

    private static final String PROCURAR_CONTA =
        "SELECT id, estado, nome_completo FROM utilizador WHERE lower(btrim(email)) = lower(btrim(?))";

    private static final String CRIAR_PENDENTE =
        "INSERT INTO utilizador (email, nome_completo, palavra_passe_hash) VALUES (lower(btrim(?)), btrim(?), ?) ON CONFLICT DO NOTHING RETURNING id";

    private static final String EMITIDO_RECENTEMENTE =
        "SELECT EXISTS (SELECT 1 FROM token_utilizador WHERE utilizador_id = ? AND tipo = ? AND criado_em > now() - make_interval(secs => ?))";

    private static final String INVALIDAR_ACTIVOS =
        "UPDATE token_utilizador SET invalidado_em = now() WHERE utilizador_id = ? AND tipo = ? AND invalidado_em IS NULL";

    private static final String GUARDAR_TOKEN =
        "INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES (?, ?, ?, now() + make_interval(secs => ?))";

    private static final String CONSUMIR_TOKEN =
        "SELECT consumir_token(?, ?)";

    private static final String ACTIVAR_CONTA =
        "UPDATE utilizador SET palavra_passe_hash = ?, estado = 'ACTIVO' WHERE id = ? AND estado = 'PENDENTE'";

    private final JdbcTemplate jdbc;

    public RepositorioDeContas(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ContaExistente> procurarPorEmail(String email) {
        List<ContaExistente> encontradas = jdbc.query(
            PROCURAR_CONTA,
            (rs, linha) -> new ContaExistente(
                rs.getObject("id", UUID.class),
                rs.getString("estado"),
                rs.getString("nome_completo")),
            email);
        return encontradas.stream().findFirst();
    }

    public Optional<UUID> criarPendente(String email, String nomeCompleto, String hashInutilizavel) {
        List<UUID> criadas = jdbc.queryForList(CRIAR_PENDENTE, UUID.class, email, nomeCompleto, hashInutilizavel);
        return criadas.stream().findFirst();
    }

    public boolean emitidoRecentemente(UUID utilizadorId, String tipo, long segundos) {
        Boolean recente = jdbc.queryForObject(
            EMITIDO_RECENTEMENTE, Boolean.class, utilizadorId, tipo, (double) segundos);
        return Boolean.TRUE.equals(recente);
    }

    public void invalidarActivos(UUID utilizadorId, String tipo) {
        jdbc.update(INVALIDAR_ACTIVOS, utilizadorId, tipo);
    }

    public void guardarToken(UUID utilizadorId, String tipo, String hashToken, long validadeSegundos) {
        jdbc.update(GUARDAR_TOKEN, utilizadorId, tipo, hashToken, (double) validadeSegundos);
    }

    public Optional<UUID> consumirToken(String hashToken, String tipo) {
        return Optional.ofNullable(jdbc.queryForObject(CONSUMIR_TOKEN, UUID.class, hashToken, tipo));
    }

    public boolean activarConta(String hashPalavraPasse, UUID utilizadorId) {
        return jdbc.update(ACTIVAR_CONTA, hashPalavraPasse, utilizadorId) == 1;
    }
}

package mz.plataformaacademica.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EstadoController {

    private final JdbcTemplate jdbc;

    public EstadoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        Integer migracoes = jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);
        String ultima = jdbc.queryForObject(
            "SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1",
            String.class);
        String postgresql = jdbc.queryForObject(
            "SELECT current_setting('server_version')", String.class);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("estado", "ok");
        resposta.put("migracoes", migracoes);
        resposta.put("ultimaMigracao", ultima);
        resposta.put("postgresql", postgresql);
        return resposta;
    }
}

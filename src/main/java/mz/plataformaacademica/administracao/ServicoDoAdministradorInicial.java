package mz.plataformaacademica.administracao;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.plataformaacademica.comum.PoliticaDePalavraPasse;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@Service
public class ServicoDoAdministradorInicial {

    public enum Resultado {
        CRIADO,
        JA_EXISTE,
        JA_EXISTE_COM_CONFIGURACAO,
        SEM_CONFIGURACAO
    }

    static final long CHAVE_DO_BLOQUEIO = 7201026001L;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final String BLOQUEAR =
        "SELECT pg_advisory_xact_lock(?)";

    private static final String EXISTE_ADMINISTRADOR =
        "SELECT EXISTS (SELECT 1 FROM atribuicao_global a JOIN papel p ON p.id = a.papel_id JOIN utilizador u ON u.id = a.utilizador_id WHERE p.codigo = 'ADMIN_GERAL' AND a.estado = 'ACTIVA' AND p.estado = 'ACTIVO' AND u.estado = 'ACTIVO')";

    private static final String CRIAR_UTILIZADOR =
        "INSERT INTO utilizador (email, nome_completo, palavra_passe_hash, estado) VALUES (?, ?, ?, 'ACTIVO') ON CONFLICT DO NOTHING RETURNING id";

    private static final String ATRIBUIR_PAPEL =
        "INSERT INTO atribuicao_global (utilizador_id, papel_id) SELECT CAST(? AS uuid), id FROM papel WHERE codigo = 'ADMIN_GERAL' AND ambito = 'GLOBAL' AND estado = 'ACTIVO'";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder codificador;

    public ServicoDoAdministradorInicial(JdbcTemplate jdbc, PasswordEncoder codificador) {
        this.jdbc = jdbc;
        this.codificador = codificador;
    }

    @Transactional
    public Resultado garantir(String email, String nome, String palavraPasse) {
        boolean emailDefinido = temTexto(email);
        boolean nomeDefinido = temTexto(nome);
        boolean palavraPasseDefinida = temTexto(palavraPasse);
        boolean algumDefinido = emailDefinido || nomeDefinido || palavraPasseDefinida;
        boolean todosDefinidos = emailDefinido && nomeDefinido && palavraPasseDefinida;

        jdbc.queryForList(BLOQUEAR, CHAVE_DO_BLOQUEIO);

        if (existeAdministrador()) {
            return algumDefinido ? Resultado.JA_EXISTE_COM_CONFIGURACAO : Resultado.JA_EXISTE;
        }
        if (!algumDefinido) {
            return Resultado.SEM_CONFIGURACAO;
        }
        if (!todosDefinidos) {
            throw new RegraDeNegocioException(
                "Configuração do administrador inicial incompleta: defina ADMIN_EMAIL, ADMIN_NOME e ADMIN_PALAVRA_PASSE");
        }

        String endereco = emailValido(email);
        String nomeCompleto = nomeValido(nome);
        PoliticaDePalavraPasse.validar(palavraPasse);

        List<UUID> criados = jdbc.queryForList(
            CRIAR_UTILIZADOR, UUID.class, endereco, nomeCompleto, codificador.encode(palavraPasse));
        if (criados.isEmpty()) {
            throw new RegraDeNegocioException(
                "O email configurado para o administrador inicial já pertence a uma conta existente");
        }
        int atribuidos = jdbc.update(ATRIBUIR_PAPEL, criados.get(0));
        if (atribuidos != 1) {
            throw new IllegalStateException("O papel ADMIN_GERAL não existe ou não está activo");
        }
        return Resultado.CRIADO;
    }

    private boolean existeAdministrador() {
        Boolean existe = jdbc.queryForObject(EXISTE_ADMINISTRADOR, Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String emailValido(String email) {
        String endereco = email.strip().toLowerCase(Locale.ROOT);
        if (endereco.length() > 255 || !EMAIL.matcher(endereco).matches()) {
            throw new RegraDeNegocioException("O email configurado para o administrador inicial não é válido");
        }
        return endereco;
    }

    private static String nomeValido(String nome) {
        String nomeCompleto = nome.strip();
        if (nomeCompleto.length() > 200) {
            throw new RegraDeNegocioException("O nome configurado para o administrador inicial é demasiado longo");
        }
        return nomeCompleto;
    }
}

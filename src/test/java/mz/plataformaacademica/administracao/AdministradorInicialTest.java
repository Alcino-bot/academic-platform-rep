package mz.plataformaacademica.administracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import mz.plataformaacademica.administracao.ServicoDoAdministradorInicial.Resultado;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdministradorInicialTest {

    private static final String PALAVRA_PASSE = "UmaSenhaBoa123";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder codificador;

    @Autowired
    private ServicoDoAdministradorInicial servico;

    private MockMvc mvc;
    private String email;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        email = "admin-" + UUID.randomUUID() + "@example.org";
        jdbc.update(
            "DELETE FROM atribuicao_global WHERE papel_id IN (SELECT id FROM papel WHERE codigo = 'ADMIN_GERAL')");
    }

    private int administradoresActivos() {
        return jdbc.queryForObject(
            "SELECT count(*) FROM atribuicao_global a JOIN papel p ON p.id = a.papel_id WHERE p.codigo = 'ADMIN_GERAL' AND a.estado = 'ACTIVA'",
            Integer.class);
    }

    private int contasComEmail(String endereco) {
        return jdbc.queryForObject(
            "SELECT count(*) FROM utilizador WHERE lower(btrim(email)) = lower(btrim(?))",
            Integer.class, endereco);
    }

    @Test
    void criaOAdministradorQuandoNaoExisteNenhum() {
        Resultado resultado = servico.garantir(email, "Admin Inicial", PALAVRA_PASSE);

        assertThat(resultado).isEqualTo(Resultado.CRIADO);
        assertThat(administradoresActivos()).isEqualTo(1);
        String estado = jdbc.queryForObject("SELECT estado FROM utilizador WHERE email = ?", String.class, email);
        assertThat(estado).isEqualTo("ACTIVO");
        String hash = jdbc.queryForObject("SELECT palavra_passe_hash FROM utilizador WHERE email = ?", String.class, email);
        assertThat(hash).isNotEqualTo(PALAVRA_PASSE);
        assertThat(codificador.matches(PALAVRA_PASSE, hash)).isTrue();
    }

    @Test
    void oAdministradorCriadoEntraEFicaComOPapelGlobal() throws Exception {
        servico.garantir(email, "Admin Inicial", PALAVRA_PASSE);

        MvcResult resultado = mvc.perform(post("/api/auth/entrar")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"palavraPasse\":\"" + PALAVRA_PASSE + "\"}")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
            .contains("PAPEL_GLOBAL_ADMIN_GERAL");
    }

    @Test
    void oEmailEGuardadoNormalizado() {
        servico.garantir("  " + email.toUpperCase() + " ", "Admin Inicial", PALAVRA_PASSE);

        assertThat(contasComEmail(email)).isEqualTo(1);
        String guardado = jdbc.queryForObject(
            "SELECT u.email FROM utilizador u JOIN atribuicao_global a ON a.utilizador_id = u.id", String.class);
        assertThat(guardado).isEqualTo(email);
    }

    @Test
    void aSegundaExecucaoNaoDuplicaNada() {
        servico.garantir(email, "Admin Inicial", PALAVRA_PASSE);

        Resultado segunda = servico.garantir(email, "Admin Inicial", PALAVRA_PASSE);

        assertThat(segunda).isEqualTo(Resultado.JA_EXISTE_COM_CONFIGURACAO);
        assertThat(contasComEmail(email)).isEqualTo(1);
        assertThat(administradoresActivos()).isEqualTo(1);
    }

    @Test
    void comAdministradorExistenteNaoCriaOutroComOutroEmail() {
        servico.garantir(email, "Admin Inicial", PALAVRA_PASSE);
        String outro = "outro-" + UUID.randomUUID() + "@example.org";

        Resultado resultado = servico.garantir(outro, "Outro Admin", PALAVRA_PASSE);

        assertThat(resultado).isEqualTo(Resultado.JA_EXISTE_COM_CONFIGURACAO);
        assertThat(contasComEmail(outro)).isEqualTo(0);
        assertThat(administradoresActivos()).isEqualTo(1);
    }

    @Test
    void comAdministradorExistenteESemVariaveisNaoFazNada() {
        servico.garantir(email, "Admin Inicial", PALAVRA_PASSE);

        Resultado resultado = servico.garantir("", "", "");

        assertThat(resultado).isEqualTo(Resultado.JA_EXISTE);
        assertThat(administradoresActivos()).isEqualTo(1);
    }

    @Test
    void semAdministradorESemVariaveisNaoCriaNadaEAvisa() {
        Resultado resultado = servico.garantir(null, "  ", "");

        assertThat(resultado).isEqualTo(Resultado.SEM_CONFIGURACAO);
        assertThat(administradoresActivos()).isEqualTo(0);
    }

    @Test
    void variaveisParciaisFalhamEnaoCriamNada() {
        assertThatThrownBy(() -> servico.garantir(email, "Admin Inicial", ""))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("incompleta");

        assertThat(contasComEmail(email)).isEqualTo(0);
        assertThat(administradoresActivos()).isEqualTo(0);
    }

    @Test
    void palavraPasseCurtaFalhaEnaoCriaNada() {
        assertThatThrownBy(() -> servico.garantir(email, "Admin Inicial", "curta123"))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("pelo menos");

        assertThat(contasComEmail(email)).isEqualTo(0);
        assertThat(administradoresActivos()).isEqualTo(0);
    }

    @Test
    void palavraPasseAcimaDe72BytesFalhaEnaoCriaNada() {
        String longa = "a".repeat(73);

        assertThatThrownBy(() -> servico.garantir(email, "Admin Inicial", longa))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("demasiado longa");

        assertThat(contasComEmail(email)).isEqualTo(0);
    }

    @Test
    void emailInvalidoFalhaEnaoCriaNada() {
        assertThatThrownBy(() -> servico.garantir("isto-nao-e-um-email", "Admin Inicial", PALAVRA_PASSE))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("não é válido");

        assertThat(administradoresActivos()).isEqualTo(0);
    }

    @Test
    void emailDeContaJaExistenteFalhaENaoPromoveAConta() {
        jdbc.update(
            "INSERT INTO utilizador (id, email, nome_completo, palavra_passe_hash, estado) VALUES (?, ?, 'Conta Normal', ?, 'ACTIVO')",
            UUID.randomUUID(), email, codificador.encode(PALAVRA_PASSE));

        assertThatThrownBy(() -> servico.garantir(email, "Admin Inicial", PALAVRA_PASSE))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("já pertence");

        assertThat(contasComEmail(email)).isEqualTo(1);
        assertThat(administradoresActivos()).isEqualTo(0);
    }

    @Test
    void administradorSuspensoNaoContaComoAdministradorExistente() {
        servico.garantir(email, "Admin Inicial", PALAVRA_PASSE);
        jdbc.update("UPDATE utilizador SET estado = 'SUSPENSO' WHERE email = ?", email);
        String novo = "novo-" + UUID.randomUUID() + "@example.org";

        Resultado resultado = servico.garantir(novo, "Novo Admin", PALAVRA_PASSE);

        assertThat(resultado).isEqualTo(Resultado.CRIADO);
        assertThat(contasComEmail(novo)).isEqualTo(1);
    }

    @Test
    void oArranqueNaoEscreveAPalavraPasseNoRegisto() {
        Logger logger = (Logger) LoggerFactory.getLogger(AdministradorInicialRunner.class);
        ListAppender<ILoggingEvent> capturados = new ListAppender<>();
        capturados.start();
        logger.addAppender(capturados);
        try {
            AdministradorInicialRunner arranque = new AdministradorInicialRunner(
                servico, email, "Admin Inicial", PALAVRA_PASSE);
            arranque.run(null);
            arranque.run(null);
        } finally {
            logger.detachAppender(capturados);
        }

        List<String> mensagens = capturados.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(mensagens.size()).isEqualTo(2);
        for (String mensagem : mensagens) {
            assertThat(mensagem).doesNotContain(PALAVRA_PASSE);
            assertThat(mensagem).doesNotContain(email);
        }
        assertThat(mensagens.get(0)).contains("criado");
    }

    @Test
    void oArranqueFalhaQuandoAConfiguracaoEInvalida() {
        AdministradorInicialRunner arranque = new AdministradorInicialRunner(
            servico, email, "Admin Inicial", "curta");

        assertThatThrownBy(() -> arranque.run(null)).isInstanceOf(RegraDeNegocioException.class);

        assertThat(administradoresActivos()).isEqualTo(0);
    }
}

package mz.plataformaacademica.conta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContasTest {

    private static final String PALAVRA_PASSE = "UmaSenhaBoa123";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EnviadorDeEmailPorLog enviador;

    private MockMvc mvc;
    private String email;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        enviador.limpar();
        email = "nova-" + UUID.randomUUID() + "@example.org";
    }

    private MvcResult registar(String nome, String enderecoEmail) throws Exception {
        return mvc.perform(post("/api/contas/registar")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nomeCompleto\":\"" + nome + "\",\"email\":\"" + enderecoEmail + "\"}")).andReturn();
    }

    private MvcResult reenviar(String enderecoEmail) throws Exception {
        return mvc.perform(post("/api/contas/reenviar-verificacao")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + enderecoEmail + "\"}")).andReturn();
    }

    private MvcResult activar(String token, String palavraPasse) throws Exception {
        return mvc.perform(post("/api/contas/activar")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\",\"palavraPasse\":\"" + palavraPasse + "\"}")).andReturn();
    }

    private MvcResult entrar(String enderecoEmail, String palavraPasse) throws Exception {
        return mvc.perform(post("/api/auth/entrar")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + enderecoEmail + "\",\"palavraPasse\":\"" + palavraPasse + "\"}")).andReturn();
    }

    private String ultimoToken() {
        List<EnviadorDeEmailPorLog.MensagemEnviada> mensagens = enviador.enviadas();
        assertThat(mensagens.isEmpty()).isFalse();
        String texto = mensagens.get(mensagens.size() - 1).texto();
        Matcher encontrado = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(texto);
        assertThat(encontrado.find()).isTrue();
        return encontrado.group(1);
    }

    private String estadoDa(String enderecoEmail) {
        return jdbc.queryForObject(
            "SELECT estado FROM utilizador WHERE lower(btrim(email)) = lower(btrim(?))",
            String.class, enderecoEmail);
    }

    private String corpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void registarCriaContaPendenteEEnviaALigacaoDeActivacao() throws Exception {
        MvcResult resultado = registar("Ana Teste", email);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(202);
        assertThat(estadoDa(email)).isEqualTo("PENDENTE");
        assertThat(enviador.enviadas().size()).isEqualTo(1);
        assertThat(enviador.enviadas().get(0).destinatario()).isEqualTo(email);
        assertThat(enviador.enviadas().get(0).texto()).contains("/activar-conta?token=");
    }

    @Test
    void registoSemTokenCsrfEProibido() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/contas/registar")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nomeCompleto\":\"Ana\",\"email\":\"" + email + "\"}")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void emailInvalidoERecusado() throws Exception {
        MvcResult resultado = registar("Ana Teste", "isto-nao-e-um-email");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(enviador.enviadas().isEmpty()).isTrue();
    }

    @Test
    void emailDeContaJaActivaDaAMesmaRespostaENaoEnviaNada() throws Exception {
        String existente = "existente-" + UUID.randomUUID() + "@example.org";
        jdbc.update(
            "INSERT INTO utilizador (id, email, nome_completo, palavra_passe_hash, estado) VALUES (?, ?, 'Existente', ?, 'ACTIVO')",
            UUID.randomUUID(), existente, new BCryptPasswordEncoder().encode(PALAVRA_PASSE));

        MvcResult novo = registar("Ana Teste", email);
        int enviadasAposNovo = enviador.enviadas().size();
        MvcResult repetido = registar("Outra Pessoa", existente);

        assertThat(repetido.getResponse().getStatus()).isEqualTo(202);
        assertThat(corpo(repetido)).isEqualTo(corpo(novo));
        assertThat(enviador.enviadas().size()).isEqualTo(enviadasAposNovo);
        assertThat(estadoDa(existente)).isEqualTo("ACTIVO");
    }

    @Test
    void segundoRegistoDentroDoIntervaloNaoReenvia() throws Exception {
        registar("Ana Teste", email);
        MvcResult segundo = registar("Ana Teste", email);

        assertThat(segundo.getResponse().getStatus()).isEqualTo(202);
        assertThat(enviador.enviadas().size()).isEqualTo(1);
    }

    @Test
    void emailComMaiusculasEEspacosCorrespondeAMesmaConta() throws Exception {
        registar("Ana Teste", "  " + email.toUpperCase() + " ");
        registar("Ana Teste", email);

        Integer contas = jdbc.queryForObject(
            "SELECT count(*) FROM utilizador WHERE lower(btrim(email)) = ?", Integer.class, email);
        assertThat(contas).isEqualTo(1);
        assertThat(enviador.enviadas().size()).isEqualTo(1);
    }

    @Test
    void activarComTokenValidoPermiteEntrar() throws Exception {
        registar("Ana Teste", email);
        String token = ultimoToken();

        MvcResult activacao = activar(token, PALAVRA_PASSE);

        assertThat(activacao.getResponse().getStatus()).isEqualTo(200);
        assertThat(estadoDa(email)).isEqualTo("ACTIVO");
        assertThat(entrar(email, PALAVRA_PASSE).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void contaPendenteNaoConsegueEntrar() throws Exception {
        registar("Ana Teste", email);

        assertThat(entrar(email, PALAVRA_PASSE).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void tokenInventadoERecusado() throws Exception {
        MvcResult resultado = activar("token-inventado", PALAVRA_PASSE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    void tokenSoPodeSerUsadoUmaVez() throws Exception {
        registar("Ana Teste", email);
        String token = ultimoToken();
        assertThat(activar(token, PALAVRA_PASSE).getResponse().getStatus()).isEqualTo(200);

        MvcResult segunda = activar(token, "OutraSenhaBoa456");

        assertThat(segunda.getResponse().getStatus()).isEqualTo(422);
        assertThat(entrar(email, PALAVRA_PASSE).getResponse().getStatus()).isEqualTo(200);
        assertThat(entrar(email, "OutraSenhaBoa456").getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void tokenExpiradoERecusado() throws Exception {
        registar("Ana Teste", email);
        String token = ultimoToken();
        jdbc.update("UPDATE token_utilizador SET criado_em = now() - interval '2 days', expira_em = now() - interval '1 day'");

        MvcResult resultado = activar(token, PALAVRA_PASSE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(estadoDa(email)).isEqualTo("PENDENTE");
    }

    @Test
    void palavraPasseFracaERecusadaSemGastarOToken() throws Exception {
        registar("Ana Teste", email);
        String token = ultimoToken();

        MvcResult fraca = activar(token, "curta");

        assertThat(fraca.getResponse().getStatus()).isEqualTo(422);
        assertThat(estadoDa(email)).isEqualTo("PENDENTE");
        assertThat(activar(token, PALAVRA_PASSE).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void palavraPasseComMaisDe72BytesERecusada() throws Exception {
        registar("Ana Teste", email);
        String token = ultimoToken();

        MvcResult resultado = activar(token, "a".repeat(80));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    void reenvioDepoisDoIntervaloInvalidaOTokenAnterior() throws Exception {
        registar("Ana Teste", email);
        String primeiro = ultimoToken();
        jdbc.update("UPDATE token_utilizador SET criado_em = now() - interval '10 minutes'");

        MvcResult reenvio = reenviar(email);
        String segundo = ultimoToken();

        assertThat(reenvio.getResponse().getStatus()).isEqualTo(202);
        assertThat(enviador.enviadas().size()).isEqualTo(2);
        assertThat(segundo).isNotEqualTo(primeiro);
        assertThat(activar(primeiro, PALAVRA_PASSE).getResponse().getStatus()).isEqualTo(422);
        assertThat(activar(segundo, PALAVRA_PASSE).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void reenviarParaEmailDesconhecidoDaAMesmaRespostaENaoEnviaNada() throws Exception {
        MvcResult resultado = reenviar("ninguem-" + UUID.randomUUID() + "@example.org");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(202);
        assertThat(enviador.enviadas().isEmpty()).isTrue();
    }
}

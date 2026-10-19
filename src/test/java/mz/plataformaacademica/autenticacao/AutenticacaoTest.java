package mz.plataformaacademica.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
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
class AutenticacaoTest {

    private static final String PALAVRA_PASSE = "SenhaSegura1";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private UUID instituicaoA;
    private UUID instituicaoB;
    private UUID utilizadorId;
    private UUID vinculoId;
    private String email;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        UUID tipo = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO tipo_instituicao (id, nome, estado, data_aprovacao) VALUES (?, ?, 'APROVADA', now())",
            tipo, "Tipo " + tipo);
        instituicaoA = novaInstituicao(tipo, "Instituicao A");
        instituicaoB = novaInstituicao(tipo, "Instituicao B");
        email = "ana-" + UUID.randomUUID() + "@example.org";
        utilizadorId = novoUtilizador(email, "ACTIVO");
        vinculoId = novoVinculo(utilizadorId, instituicaoA, "ESTUDANTE");
    }

    private UUID novaInstituicao(UUID tipo, String nome) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO instituicao (id, nome, sigla, tipo_instituicao_id, estado) VALUES (?, ?, ?, ?, 'ACTIVA')",
            id, nome + " " + id, "I" + id.toString().substring(0, 8), tipo);
        return id;
    }

    private UUID novoUtilizador(String enderecoEmail, String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO utilizador (id, email, nome_completo, palavra_passe_hash, estado) VALUES (?, ?, 'Ana Teste', ?, ?)",
            id, enderecoEmail, new BCryptPasswordEncoder().encode(PALAVRA_PASSE), estado);
        return id;
    }

    private UUID novoVinculo(UUID utilizador, UUID instituicao, String tipoVinculo) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO vinculo_institucional (id, utilizador_id, instituicao_id, tipo_vinculo, estado) VALUES (?, ?, ?, ?, 'ACTIVO')",
            id, utilizador, instituicao, tipoVinculo);
        return id;
    }

    private String corpoEntrar(String enderecoEmail, String palavraPasse) {
        return "{\"email\":\"" + enderecoEmail + "\",\"palavraPasse\":\"" + palavraPasse + "\"}";
    }

    private MvcResult entrar(String enderecoEmail, String palavraPasse) throws Exception {
        return mvc.perform(post("/api/auth/entrar")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpoEntrar(enderecoEmail, palavraPasse))).andReturn();
    }

    private MockHttpSession entrarComSucesso() throws Exception {
        MvcResult resultado = entrar(email, PALAVRA_PASSE);
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    private MvcResult escolherInstituicao(MockHttpSession sessao, UUID instituicao) throws Exception {
        return mvc.perform(post("/api/auth/instituicao")
            .session(sessao)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"instituicaoId\":\"" + instituicao + "\"}")).andReturn();
    }

    private MvcResult eu(MockHttpSession sessao) throws Exception {
        return mvc.perform(get("/api/auth/eu").session(sessao)).andReturn();
    }

    private String corpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void csrfDevolveTokenECriaSessao() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/auth/csrf")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("token").contains("cabecalho");
        assertThat(resultado.getRequest().getSession(false)).isNotNull();
    }

    @Test
    void entrarSemTokenCsrfEProibido() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/entrar")
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpoEntrar(email, PALAVRA_PASSE))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void fluxoCsrfCompletoComTokenObtidoPorPedidoFunciona() throws Exception {
        MvcResult inicio = mvc.perform(get("/api/auth/csrf")).andReturn();
        MockHttpSession sessao = (MockHttpSession) inicio.getRequest().getSession(false);
        Matcher encontrado = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(corpo(inicio));
        assertThat(encontrado.find()).isTrue();
        String token = encontrado.group(1);
        String idAntes = sessao.getId();

        MvcResult resultado = mvc.perform(post("/api/auth/entrar")
            .session(sessao)
            .header("X-CSRF-TOKEN", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpoEntrar(email, PALAVRA_PASSE))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(sessao.getId()).isNotEqualTo(idAntes);
        assertThat(corpo(resultado)).contains("csrfToken");
    }

    @Test
    void palavraPasseErradaEEmailDesconhecidoDaoAMesmaResposta() throws Exception {
        MvcResult errada = entrar(email, "PalavraErrada1");
        MvcResult desconhecido = entrar("ninguem-" + UUID.randomUUID() + "@example.org", PALAVRA_PASSE);

        assertThat(errada.getResponse().getStatus()).isEqualTo(401);
        assertThat(desconhecido.getResponse().getStatus()).isEqualTo(401);
        assertThat(corpo(errada)).contains("inválidos");
        assertThat(corpo(desconhecido)).contains("inválidos");
    }

    @Test
    void entrarComSucessoListaAsInstituicoesDisponiveis() throws Exception {
        MvcResult resultado = entrar(email, PALAVRA_PASSE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains(instituicaoA.toString());
        assertThat(corpo(resultado)).doesNotContain(instituicaoB.toString());
        assertThat(resultado.getRequest().getSession(false)).isNotNull();
    }

    @Test
    void contaSuspensaNaoEntraMesmoComAPalavraPasseCerta() throws Exception {
        String emailSuspenso = "suspensa-" + UUID.randomUUID() + "@example.org";
        novoUtilizador(emailSuspenso, "SUSPENSO");

        MvcResult resultado = entrar(emailSuspenso, PALAVRA_PASSE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void semSessaoOPedidoEmEuEProibidoComNaoAutenticado() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/auth/eu")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void escolherInstituicaoComVinculoActivoFunciona() throws Exception {
        MockHttpSession sessao = entrarComSucesso();

        MvcResult escolha = escolherInstituicao(sessao, instituicaoA);
        MvcResult quemSou = eu(sessao);

        assertThat(escolha.getResponse().getStatus()).isEqualTo(200);
        assertThat(quemSou.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(quemSou)).contains("\"instituicaoActivaId\":\"" + instituicaoA + "\"");
        assertThat(corpo(quemSou)).contains("VINCULO_ESTUDANTE");
    }

    @Test
    void escolherInstituicaoSemVinculoEProibido() throws Exception {
        MockHttpSession sessao = entrarComSucesso();

        MvcResult escolha = escolherInstituicao(sessao, instituicaoB);
        MvcResult quemSou = eu(sessao);

        assertThat(escolha.getResponse().getStatus()).isEqualTo(403);
        assertThat(corpo(quemSou)).contains("\"instituicaoActivaId\":null");
    }

    @Test
    void papelRevogadoDeixaDeValerNoPedidoSeguinte() throws Exception {
        jdbc.update(
            "INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id) SELECT ?, ?, id FROM papel WHERE codigo = 'ESTUDANTE'",
            vinculoId, instituicaoA);
        MockHttpSession sessao = entrarComSucesso();
        escolherInstituicao(sessao, instituicaoA);

        assertThat(corpo(eu(sessao))).contains("PAPEL_ESTUDANTE");

        jdbc.update(
            "UPDATE vinculo_papel SET estado = 'REVOGADO', data_revogacao = now() WHERE vinculo_id = ?", vinculoId);

        assertThat(corpo(eu(sessao))).doesNotContain("PAPEL_ESTUDANTE");
    }

    @Test
    void vinculoTerminadoRetiraAInstituicaoActivaNoPedidoSeguinte() throws Exception {
        MockHttpSession sessao = entrarComSucesso();
        escolherInstituicao(sessao, instituicaoA);
        assertThat(corpo(eu(sessao))).contains("\"instituicaoActivaId\":\"" + instituicaoA + "\"");

        jdbc.update(
            "UPDATE vinculo_institucional SET estado = 'TERMINADO', data_fim = CURRENT_DATE WHERE id = ?", vinculoId);

        MvcResult depois = eu(sessao);
        assertThat(depois.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(depois)).contains("\"instituicaoActivaId\":null");
    }

    @Test
    void contaSuspensaDuranteASessaoPerdeOAcesso() throws Exception {
        MockHttpSession sessao = entrarComSucesso();
        assertThat(eu(sessao).getResponse().getStatus()).isEqualTo(200);

        jdbc.update("UPDATE utilizador SET estado = 'SUSPENSO' WHERE id = ?", utilizadorId);

        assertThat(eu(sessao).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void cincoFalhasBloqueiamAContaAteExpirarOBloqueio() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(entrar(email, "PalavraErrada" + i).getResponse().getStatus()).isEqualTo(401);
        }

        Boolean bloqueada = jdbc.queryForObject(
            "SELECT bloqueado_ate IS NOT NULL AND bloqueado_ate > now() FROM utilizador WHERE id = ?",
            Boolean.class, utilizadorId);
        assertThat(bloqueada).isTrue();
        assertThat(entrar(email, PALAVRA_PASSE).getResponse().getStatus()).isEqualTo(401);

        jdbc.update("UPDATE utilizador SET bloqueado_ate = now() - interval '1 minute' WHERE id = ?", utilizadorId);

        assertThat(entrar(email, PALAVRA_PASSE).getResponse().getStatus()).isEqualTo(200);
        Integer tentativas = jdbc.queryForObject(
            "SELECT tentativas_falhadas FROM utilizador WHERE id = ?", Integer.class, utilizadorId);
        assertThat(tentativas).isEqualTo(0);
    }

    @Test
    void sairInvalidaASessao() throws Exception {
        MockHttpSession sessao = entrarComSucesso();
        assertThat(eu(sessao).getResponse().getStatus()).isEqualTo(200);

        MvcResult saida = mvc.perform(post("/api/auth/sair").session(sessao).with(csrf())).andReturn();

        assertThat(saida.getResponse().getStatus()).isEqualTo(204);
        assertThat(eu(sessao).getResponse().getStatus()).isEqualTo(401);
    }
}

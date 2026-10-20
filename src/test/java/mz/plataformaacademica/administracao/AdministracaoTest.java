package mz.plataformaacademica.administracao;

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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdministracaoTest {

    private static final String PALAVRA_PASSE = "SenhaSegura1";
    private static final Pattern IDENTIFICADOR = Pattern.compile("\"id\":\"([0-9a-f-]{36})\"");

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager em;

    private MockMvc mvc;
    private UUID administradorId;
    private String emailAdministrador;
    private String emailComum;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        emailAdministrador = "admin-" + UUID.randomUUID() + "@example.org";
        emailComum = "comum-" + UUID.randomUUID() + "@example.org";
        administradorId = novoUtilizador(emailAdministrador);
        jdbc.update(
            "INSERT INTO atribuicao_global (utilizador_id, papel_id) SELECT CAST(? AS uuid), id FROM papel WHERE codigo = 'ADMIN_GERAL'",
            administradorId);
        novoUtilizador(emailComum);
    }

    private UUID novoUtilizador(String email) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO utilizador (id, email, nome_completo, palavra_passe_hash, estado) VALUES (?, ?, 'Pessoa Teste', ?, 'ACTIVO')",
            id, email, new BCryptPasswordEncoder().encode(PALAVRA_PASSE));
        return id;
    }

    private MockHttpSession entrar(String email) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/entrar")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"palavraPasse\":\"" + PALAVRA_PASSE + "\"}")).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    private MvcResult pedirGet(MockHttpSession sessao, String url) throws Exception {
        return mvc.perform(get(url).session(sessao)).andReturn();
    }

    private MvcResult pedirPost(MockHttpSession sessao, String url, String json) throws Exception {
        return mvc.perform(post(url)
            .session(sessao)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)).andReturn();
    }

    private String corpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private int estado(MvcResult resultado) {
        return resultado.getResponse().getStatus();
    }

    private UUID identificador(MvcResult resultado) throws Exception {
        Matcher encontrado = IDENTIFICADOR.matcher(corpo(resultado));
        assertThat(encontrado.find()).isTrue();
        return UUID.fromString(encontrado.group(1));
    }

    private UUID tipoNaBase(String estadoDoTipo) {
        UUID id = UUID.randomUUID();
        if ("APROVADA".equals(estadoDoTipo)) {
            jdbc.update(
                "INSERT INTO tipo_instituicao (id, nome, estado, data_aprovacao) VALUES (?, ?, 'APROVADA', now())",
                id, "Tipo " + id);
        } else {
            jdbc.update("INSERT INTO tipo_instituicao (id, nome) VALUES (?, ?)", id, "Tipo " + id);
        }
        return id;
    }

    private UUID modeloNaBase(UUID tipo, String estadoDoModelo) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO modelo_organizacional (id, tipo_instituicao_id, nome, estado) VALUES (?, ?, ?, ?)",
            id, tipo, "Modelo " + id, estadoDoModelo);
        return id;
    }

    private UUID tipoDeUnidadeDoModelo(UUID modelo, String nome, int nivel, UUID pai) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO modelo_tipo_unidade (id, modelo_id, nome, nivel, tipo_pai_id) VALUES (?, ?, ?, ?, ?)",
            id, modelo, nome, nivel, pai);
        return id;
    }

    private UUID modeloActivoComHierarquia() {
        UUID modelo = modeloNaBase(tipoNaBase("APROVADA"), "ACTIVO");
        UUID faculdade = tipoDeUnidadeDoModelo(modelo, "Faculdade", 1, null);
        UUID departamento = tipoDeUnidadeDoModelo(modelo, "Departamento", 2, faculdade);
        tipoDeUnidadeDoModelo(modelo, "Curso", 3, departamento);
        return modelo;
    }

    private String corpoInstituicao(String nome, String sigla, UUID modelo) {
        return "{\"nome\":\"" + nome + "\",\"sigla\":\"" + sigla + "\",\"modeloId\":\"" + modelo + "\"}";
    }

    private String corpoModelo(UUID tipo, String nome) {
        return "{\"tipoInstituicaoId\":\"" + tipo + "\",\"nome\":\"" + nome + "\"}";
    }

    private String corpoTipoUnidade(String nome, int nivel, UUID pai) {
        String paiJson = pai == null ? "null" : "\"" + pai + "\"";
        return "{\"nome\":\"" + nome + "\",\"nivel\":" + nivel + ",\"tipoPaiId\":" + paiJson + "}";
    }

    private UUID criarModeloViaApi(MockHttpSession sessao, UUID tipo, String nome) throws Exception {
        MvcResult resultado = pedirPost(
            sessao, "/api/administracao/modelos-organizacionais", corpoModelo(tipo, nome));
        assertThat(estado(resultado)).isEqualTo(201);
        return identificador(resultado);
    }

    private UUID adicionarTipoViaApi(
            MockHttpSession sessao, UUID modelo, String nome, int nivel, UUID pai) throws Exception {
        MvcResult resultado = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais/" + modelo + "/tipos-unidade",
            corpoTipoUnidade(nome, nivel, pai));
        assertThat(estado(resultado)).isEqualTo(201);
        return identificador(resultado);
    }

    private UUID modeloActivoViaApi(MockHttpSession sessao, UUID tipo) throws Exception {
        UUID modelo = criarModeloViaApi(sessao, tipo, "Modelo " + UUID.randomUUID());
        adicionarTipoViaApi(sessao, modelo, "Unidade " + UUID.randomUUID(), 1, null);
        MvcResult activar = pedirPost(
            sessao, "/api/administracao/modelos-organizacionais/" + modelo + "/activar", "{}");
        assertThat(estado(activar)).isEqualTo(200);
        return modelo;
    }

    private int contar(String sql, Object... argumentos) {
        return jdbc.queryForObject(sql, Integer.class, argumentos);
    }

    @Test
    void administradorListaModelosOrganizacionais() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        String nome = "Modelo Listado " + UUID.randomUUID();
        criarModeloViaApi(sessao, tipoNaBase("APROVADA"), nome);

        MvcResult resultado = pedirGet(sessao, "/api/administracao/modelos-organizacionais");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains(nome);
    }

    @Test
    void obterModeloInexistenteDa404() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = pedirGet(
            sessao, "/api/administracao/modelos-organizacionais/" + UUID.randomUUID());

        assertThat(estado(resultado)).isEqualTo(404);
    }

    @Test
    void criarModeloDeixaRascunhoComNomeNormalizado() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");
        String nome = "Modelo Novo " + UUID.randomUUID();

        MvcResult resultado = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais",
            corpoModelo(tipo, "  " + nome + "  "));

        assertThat(estado(resultado)).isEqualTo(201);
        UUID modelo = identificador(resultado);
        assertThat(contar(
            "SELECT count(*) FROM modelo_organizacional WHERE id = ? AND tipo_instituicao_id = ? AND nome = ? AND estado = 'RASCUNHO'",
            modelo, tipo, nome)).isEqualTo(1);
    }

    @Test
    void criarModeloComTipoInexistenteDa404() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais",
            corpoModelo(UUID.randomUUID(), "Modelo sem tipo"));

        assertThat(estado(resultado)).isEqualTo(404);
    }

    @Test
    void nomeDeModeloRepetidoNoMesmoTipoDa409() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");
        String nome = "Modelo Repetido " + UUID.randomUUID();
        criarModeloViaApi(sessao, tipo, nome);

        MvcResult repetido = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais",
            corpoModelo(tipo, nome.toUpperCase()));

        assertThat(estado(repetido)).isEqualTo(409);
        assertThat(contar(
            "SELECT count(*) FROM modelo_organizacional WHERE tipo_instituicao_id = ? AND lower(nome) = lower(?)",
            tipo, nome)).isEqualTo(1);
    }

    @Test
    void obterModeloIncluiTiposDeUnidadeEPai() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Modelo " + UUID.randomUUID());
        String nomeRaiz = "Raiz " + UUID.randomUUID();
        UUID raiz = adicionarTipoViaApi(sessao, modelo, nomeRaiz, 1, null);
        String nomeFilho = "Filho " + UUID.randomUUID();
        adicionarTipoViaApi(sessao, modelo, nomeFilho, 2, raiz);

        MvcResult resultado = pedirGet(
            sessao, "/api/administracao/modelos-organizacionais/" + modelo);

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains(nomeRaiz, nomeFilho, raiz.toString());
    }

    @Test
    void adicionarTipoDeUnidadeDeNivelUmSemPai() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Modelo " + UUID.randomUUID());
        String nome = "Raiz " + UUID.randomUUID();

        MvcResult resultado = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais/" + modelo + "/tipos-unidade",
            corpoTipoUnidade(nome, 1, null));

        assertThat(estado(resultado)).isEqualTo(201);
        assertThat(contar(
            "SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ? AND nome = ? AND nivel = 1 AND tipo_pai_id IS NULL",
            modelo, nome)).isEqualTo(1);
    }

    @Test
    void adicionarTipoDeUnidadeFilhoMantemHierarquia() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Modelo " + UUID.randomUUID());
        UUID pai = adicionarTipoViaApi(sessao, modelo, "Pai " + UUID.randomUUID(), 1, null);
        String nome = "Filho " + UUID.randomUUID();

        MvcResult resultado = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais/" + modelo + "/tipos-unidade",
            corpoTipoUnidade(nome, 2, pai));

        assertThat(estado(resultado)).isEqualTo(201);
        assertThat(contar(
            "SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ? AND nome = ? AND nivel = 2 AND tipo_pai_id = ?",
            modelo, nome, pai)).isEqualTo(1);
    }

    @Test
    void adicionarTipoDeUnidadeComPaiDeOutroModeloDa404() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Modelo " + UUID.randomUUID());
        UUID outroModelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Outro " + UUID.randomUUID());
        UUID pai = adicionarTipoViaApi(sessao, outroModelo, "Pai " + UUID.randomUUID(), 1, null);

        MvcResult resultado = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais/" + modelo + "/tipos-unidade",
            corpoTipoUnidade("Filho " + UUID.randomUUID(), 2, pai));

        assertThat(estado(resultado)).isEqualTo(404);
        assertThat(contar("SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ?", modelo)).isEqualTo(0);
    }

    @Test
    void adicionarTipoDeUnidadeComNivelQueNaoSegueOPaiDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Modelo " + UUID.randomUUID());
        UUID pai = adicionarTipoViaApi(sessao, modelo, "Pai " + UUID.randomUUID(), 1, null);

        MvcResult resultado = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais/" + modelo + "/tipos-unidade",
            corpoTipoUnidade("Filho " + UUID.randomUUID(), 3, pai));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ?", modelo)).isEqualTo(1);
    }

    @Test
    void adicionarTipoAcimaDoNivelUmSemPaiDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Modelo " + UUID.randomUUID());

        MvcResult resultado = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais/" + modelo + "/tipos-unidade",
            corpoTipoUnidade("Filho " + UUID.randomUUID(), 2, null));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ?", modelo)).isEqualTo(0);
    }

    @Test
    void adicionarTipoDeNivelUmComPaiDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Modelo " + UUID.randomUUID());
        UUID pai = adicionarTipoViaApi(sessao, modelo, "Pai " + UUID.randomUUID(), 1, null);

        MvcResult resultado = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais/" + modelo + "/tipos-unidade",
            corpoTipoUnidade("Outra raiz " + UUID.randomUUID(), 1, pai));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ?", modelo)).isEqualTo(1);
    }

    @Test
    void adicionarTipoDeUnidadeDuplicadoDa409() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Modelo " + UUID.randomUUID());
        String nome = "Unidade " + UUID.randomUUID();
        adicionarTipoViaApi(sessao, modelo, nome, 1, null);

        MvcResult repetido = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais/" + modelo + "/tipos-unidade",
            corpoTipoUnidade(nome.toUpperCase(), 1, null));

        assertThat(estado(repetido)).isEqualTo(409);
        assertThat(contar("SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ?", modelo)).isEqualTo(1);
    }

    @Test
    void activarModeloSemTiposDeUnidadeDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Modelo " + UUID.randomUUID());

        MvcResult resultado = pedirPost(
            sessao, "/api/administracao/modelos-organizacionais/" + modelo + "/activar", "{}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar(
            "SELECT count(*) FROM modelo_organizacional WHERE id = ? AND estado = 'RASCUNHO'", modelo)).isEqualTo(1);
    }

    @Test
    void activarModeloDeTipoNaoAprovadoDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("PENDENTE"), "Modelo " + UUID.randomUUID());
        adicionarTipoViaApi(sessao, modelo, "Unidade " + UUID.randomUUID(), 1, null);

        MvcResult resultado = pedirPost(
            sessao, "/api/administracao/modelos-organizacionais/" + modelo + "/activar", "{}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar(
            "SELECT count(*) FROM modelo_organizacional WHERE id = ? AND estado = 'RASCUNHO'", modelo)).isEqualTo(1);
    }

    @Test
    void activarModeloValidoPermiteCriarInstituicao() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Modelo " + UUID.randomUUID());
        adicionarTipoViaApi(sessao, modelo, "Unidade " + UUID.randomUUID(), 1, null);

        MvcResult activar = pedirPost(
            sessao, "/api/administracao/modelos-organizacionais/" + modelo + "/activar", "{}");
        MvcResult instituicao = pedirPost(
            sessao,
            "/api/administracao/instituicoes",
            corpoInstituicao(
                "Instituicao " + UUID.randomUUID(),
                "M" + UUID.randomUUID().toString().substring(0, 8),
                modelo));

        assertThat(estado(activar)).isEqualTo(200);
        assertThat(estado(instituicao)).isEqualTo(201);
    }

    @Test
    void activarModeloRejeitaHierarquiaInvalidaNaBase() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloViaApi(sessao, tipoNaBase("APROVADA"), "Modelo " + UUID.randomUUID());
        UUID pai = adicionarTipoViaApi(sessao, modelo, "Pai " + UUID.randomUUID(), 1, null);
        UUID filho = adicionarTipoViaApi(sessao, modelo, "Filho " + UUID.randomUUID(), 2, pai);
        jdbc.update("UPDATE modelo_tipo_unidade SET nivel = 4 WHERE id = ?", filho);

        MvcResult resultado = pedirPost(
            sessao, "/api/administracao/modelos-organizacionais/" + modelo + "/activar", "{}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar(
            "SELECT count(*) FROM modelo_organizacional WHERE id = ? AND estado = 'RASCUNHO'", modelo)).isEqualTo(1);
    }

    @Test
    void modeloActivoNaoPodeSerActivadoNovamenteNemAlterado() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloActivoViaApi(sessao, tipoNaBase("APROVADA"));

        MvcResult activarNovamente = pedirPost(
            sessao, "/api/administracao/modelos-organizacionais/" + modelo + "/activar", "{}");
        MvcResult adicionar = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais/" + modelo + "/tipos-unidade",
            corpoTipoUnidade("Outra " + UUID.randomUUID(), 1, null));

        assertThat(estado(activarNovamente)).isEqualTo(422);
        assertThat(estado(adicionar)).isEqualTo(422);
        assertThat(contar(
            "SELECT count(*) FROM modelo_organizacional WHERE id = ? AND estado = 'ACTIVO'", modelo)).isEqualTo(1);
    }

    @Test
    void visitanteAnonimoNaoAcedeAAdministracao() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/administracao/tipos-instituicao")).andReturn();

        assertThat(estado(resultado)).isEqualTo(401);
    }

    @Test
    void utilizadorSemPapelGlobalRecebe403() throws Exception {
        MockHttpSession sessao = entrar(emailComum);

        assertThat(estado(pedirGet(sessao, "/api/administracao/tipos-instituicao"))).isEqualTo(403);
        assertThat(estado(pedirGet(sessao, "/api/administracao/instituicoes"))).isEqualTo(403);
        assertThat(estado(pedirGet(sessao, "/api/administracao/modelos-organizacionais"))).isEqualTo(403);
    }

    @Test
    void utilizadorSemPapelGlobalNaoCriaNemDecideNada() throws Exception {
        MockHttpSession sessao = entrar(emailComum);
        UUID modelo = modeloActivoComHierarquia();
        UUID tipoPendente = tipoNaBase("PENDENTE");
        String nome = "Instituicao " + UUID.randomUUID();

        MvcResult criar = pedirPost(sessao, "/api/administracao/instituicoes", corpoInstituicao(nome, "IT", modelo));
        MvcResult aprovar = pedirPost(sessao, "/api/administracao/tipos-instituicao/" + tipoPendente + "/aprovar", "{}");
        MvcResult criarModelo = pedirPost(
            sessao,
            "/api/administracao/modelos-organizacionais",
            corpoModelo(tipoPendente, "Modelo " + UUID.randomUUID()));

        assertThat(estado(criar)).isEqualTo(403);
        assertThat(estado(aprovar)).isEqualTo(403);
        assertThat(estado(criarModelo)).isEqualTo(403);
        assertThat(contar("SELECT count(*) FROM instituicao WHERE nome = ?", nome)).isEqualTo(0);
        assertThat(contar("SELECT count(*) FROM modelo_organizacional WHERE tipo_instituicao_id = ?", tipoPendente))
            .isEqualTo(0);
        assertThat(contar("SELECT count(*) FROM tipo_instituicao WHERE id = ? AND estado = 'PENDENTE'", tipoPendente))
            .isEqualTo(1);
    }

    @Test
    void pedidoSemTokenCsrfEProibidoMesmoParaAdministrador() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = mvc.perform(post("/api/administracao/tipos-instituicao")
            .session(sessao)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Sem Csrf\"}")).andReturn();

        assertThat(estado(resultado)).isEqualTo(403);
        assertThat(contar("SELECT count(*) FROM tipo_instituicao WHERE nome = 'Sem Csrf'")).isEqualTo(0);

        UUID tipo = tipoNaBase("APROVADA");
        MvcResult criarModelo = mvc.perform(post("/api/administracao/modelos-organizacionais")
            .session(sessao)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpoModelo(tipo, "Sem Csrf Modelo"))).andReturn();

        assertThat(estado(criarModelo)).isEqualTo(403);
        assertThat(contar("SELECT count(*) FROM modelo_organizacional WHERE tipo_instituicao_id = ?", tipo))
            .isEqualTo(0);
    }

    @Test
    void administradorListaOsTipos() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");

        MvcResult resultado = pedirGet(sessao, "/api/administracao/tipos-instituicao");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains(tipo.toString());
    }

    @Test
    void proporUmTipoCriaUmTipoPendenteComProponente() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        String nome = "Tipo Novo " + UUID.randomUUID();

        MvcResult resultado = pedirPost(sessao, "/api/administracao/tipos-instituicao",
            "{\"nome\":\"  " + nome + "  \",\"descricao\":\"Uma descricao\"}");

        assertThat(estado(resultado)).isEqualTo(201);
        UUID id = identificador(resultado);
        assertThat(contar(
            "SELECT count(*) FROM tipo_instituicao WHERE id = ? AND nome = ? AND estado = 'PENDENTE' AND proposto_por = ? AND decidido_por IS NULL",
            id, nome, administradorId)).isEqualTo(1);
    }

    @Test
    void proporUmTipoComNomeRepetidoDa409() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        String nome = "Tipo Repetido " + UUID.randomUUID();
        pedirPost(sessao, "/api/administracao/tipos-instituicao", "{\"nome\":\"" + nome + "\"}");

        MvcResult repetido = pedirPost(sessao, "/api/administracao/tipos-instituicao",
            "{\"nome\":\"" + nome.toUpperCase() + "\"}");

        assertThat(estado(repetido)).isEqualTo(409);
        assertThat(contar("SELECT count(*) FROM tipo_instituicao WHERE lower(nome) = lower(?)", nome)).isEqualTo(1);
    }

    @Test
    void aprovarUmTipoGuardaQuemDecidiuEQuando() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("PENDENTE");

        MvcResult resultado = pedirPost(sessao, "/api/administracao/tipos-instituicao/" + tipo + "/aprovar", "{}");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains("APROVADA");
        assertThat(contar(
            "SELECT count(*) FROM tipo_instituicao WHERE id = ? AND estado = 'APROVADA' AND decidido_por = ? AND data_aprovacao IS NOT NULL",
            tipo, administradorId)).isEqualTo(1);
    }

    @Test
    void rejeitarUmTipoGuardaQuemDecidiu() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("PENDENTE");

        MvcResult resultado = pedirPost(sessao, "/api/administracao/tipos-instituicao/" + tipo + "/rejeitar", "{}");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(contar(
            "SELECT count(*) FROM tipo_instituicao WHERE id = ? AND estado = 'REJEITADA' AND decidido_por = ? AND data_aprovacao IS NULL",
            tipo, administradorId)).isEqualTo(1);
    }

    @Test
    void umTipoJaDecididoNaoMudaDeEstado() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("PENDENTE");
        pedirPost(sessao, "/api/administracao/tipos-instituicao/" + tipo + "/aprovar", "{}");

        MvcResult aprovarOutraVez = pedirPost(sessao, "/api/administracao/tipos-instituicao/" + tipo + "/aprovar", "{}");
        MvcResult rejeitarDepois = pedirPost(sessao, "/api/administracao/tipos-instituicao/" + tipo + "/rejeitar", "{}");

        assertThat(estado(aprovarOutraVez)).isEqualTo(422);
        assertThat(estado(rejeitarDepois)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM tipo_instituicao WHERE id = ? AND estado = 'APROVADA'", tipo))
            .isEqualTo(1);
    }

    @Test
    void decidirUmTipoInexistenteDa404() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = pedirPost(sessao,
            "/api/administracao/tipos-instituicao/" + UUID.randomUUID() + "/aprovar", "{}");

        assertThat(estado(resultado)).isEqualTo(404);
    }

    @Test
    void criaUmaInstituicaoActivaAPartirDoModelo() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloActivoComHierarquia();
        UUID tipo = jdbc.queryForObject(
            "SELECT tipo_instituicao_id FROM modelo_organizacional WHERE id = ?", UUID.class, modelo);
        String nome = "Instituicao " + UUID.randomUUID();

        MvcResult resultado = pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao(nome, "I" + UUID.randomUUID().toString().substring(0, 8), modelo));
        em.flush();

        assertThat(estado(resultado)).isEqualTo(201);
        assertThat(corpo(resultado)).contains("ACTIVA");
        UUID id = identificador(resultado);
        assertThat(contar(
            "SELECT count(*) FROM instituicao WHERE id = ? AND estado = 'ACTIVA' AND modelo_origem_id = ? AND tipo_instituicao_id = ?",
            id, modelo, tipo)).isEqualTo(1);
    }

    @Test
    void copiaOsTiposDeUnidadeDoModeloMantendoAHierarquia() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloActivoComHierarquia();

        MvcResult resultado = pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao("Instituicao " + UUID.randomUUID(), "I" + UUID.randomUUID().toString().substring(0, 8), modelo));
        em.flush();

        UUID id = identificador(resultado);
        assertThat(contar("SELECT count(*) FROM tipo_unidade WHERE instituicao_id = ?", id)).isEqualTo(3);
        assertThat(contar(
            "SELECT count(*) FROM tipo_unidade WHERE instituicao_id = ? AND modelo_tipo_origem_id IS NOT NULL", id))
            .isEqualTo(3);
        assertThat(contar(
            "SELECT count(*) FROM tipo_unidade d JOIN tipo_unidade f ON f.id = d.tipo_pai_id AND f.instituicao_id = d.instituicao_id WHERE d.instituicao_id = ? AND d.nome = 'Departamento' AND f.nome = 'Faculdade' AND d.nivel = 2 AND f.nivel = 1",
            id)).isEqualTo(1);
        assertThat(contar(
            "SELECT count(*) FROM tipo_unidade c JOIN tipo_unidade d ON d.id = c.tipo_pai_id AND d.instituicao_id = c.instituicao_id WHERE c.instituicao_id = ? AND c.nome = 'Curso' AND d.nome = 'Departamento'",
            id)).isEqualTo(1);
    }

    @Test
    void duasInstituicoesDoMesmoModeloTemTiposDeUnidadeProprios() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloActivoComHierarquia();

        UUID primeira = identificador(pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao("Primeira " + UUID.randomUUID(), "P" + UUID.randomUUID().toString().substring(0, 8), modelo)));
        UUID segunda = identificador(pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao("Segunda " + UUID.randomUUID(), "S" + UUID.randomUUID().toString().substring(0, 8), modelo)));
        em.flush();

        assertThat(contar("SELECT count(*) FROM tipo_unidade WHERE instituicao_id = ?", primeira)).isEqualTo(3);
        assertThat(contar("SELECT count(*) FROM tipo_unidade WHERE instituicao_id = ?", segunda)).isEqualTo(3);
        assertThat(contar(
            "SELECT count(*) FROM tipo_unidade a JOIN tipo_unidade b ON b.id = a.id WHERE a.instituicao_id = ? AND b.instituicao_id = ?",
            primeira, segunda)).isEqualTo(0);
        assertThat(contar(
            "SELECT count(*) FROM tipo_unidade a WHERE a.instituicao_id = ? AND a.tipo_pai_id IN (SELECT id FROM tipo_unidade WHERE instituicao_id = ?)",
            primeira, segunda)).isEqualTo(0);
    }

    @Test
    void modeloEmRascunhoDa422ENaoCriaNada() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloNaBase(tipoNaBase("APROVADA"), "RASCUNHO");
        String nome = "Instituicao " + UUID.randomUUID();

        MvcResult resultado = pedirPost(sessao, "/api/administracao/instituicoes", corpoInstituicao(nome, "IR", modelo));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(corpo(resultado)).contains("não está activo");
        assertThat(contar("SELECT count(*) FROM instituicao WHERE nome = ?", nome)).isEqualTo(0);
    }

    @Test
    void modeloDeTipoNaoAprovadoDa422ENaoCriaNada() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloNaBase(tipoNaBase("PENDENTE"), "ACTIVO");
        String nome = "Instituicao " + UUID.randomUUID();

        MvcResult resultado = pedirPost(sessao, "/api/administracao/instituicoes", corpoInstituicao(nome, "IP", modelo));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(corpo(resultado)).contains("ainda não foi aprovado");
        assertThat(contar("SELECT count(*) FROM instituicao WHERE nome = ?", nome)).isEqualTo(0);
    }

    @Test
    void modeloInexistenteDa404() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao("Instituicao " + UUID.randomUUID(), "IX", UUID.randomUUID()));

        assertThat(estado(resultado)).isEqualTo(404);
    }

    @Test
    void nomeDeInstituicaoRepetidoDa409() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloActivoComHierarquia();
        String nome = "Instituicao Repetida " + UUID.randomUUID();
        pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao(nome, "R" + UUID.randomUUID().toString().substring(0, 8), modelo));

        MvcResult repetida = pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao(nome.toUpperCase(), "Q" + UUID.randomUUID().toString().substring(0, 8), modelo));

        assertThat(estado(repetida)).isEqualTo(409);
    }

    @Test
    void mudaOEstadoDaInstituicaoSoPorTransicoesPermitidas() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloActivoComHierarquia();
        UUID id = identificador(pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao("Instituicao " + UUID.randomUUID(), "T" + UUID.randomUUID().toString().substring(0, 8), modelo)));
        String url = "/api/administracao/instituicoes/" + id + "/estado";

        MvcResult suspender = pedirPost(sessao, url, "{\"estado\":\"SUSPENSA\"}");
        MvcResult inactivar = pedirPost(sessao, url, "{\"estado\":\"INACTIVA\"}");
        em.flush();
        MvcResult reactivar = pedirPost(sessao, url, "{\"estado\":\"ACTIVA\"}");

        assertThat(estado(suspender)).isEqualTo(200);
        assertThat(estado(inactivar)).isEqualTo(200);
        assertThat(estado(reactivar)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM instituicao WHERE id = ? AND estado = 'INACTIVA'", id)).isEqualTo(1);
    }

    @Test
    void mudarOEstadoDeInstituicaoInexistenteDa404() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = pedirPost(sessao,
            "/api/administracao/instituicoes/" + UUID.randomUUID() + "/estado", "{\"estado\":\"SUSPENSA\"}");

        assertThat(estado(resultado)).isEqualTo(404);
    }

    @Test
    void administradorListaAsInstituicoes() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloActivoComHierarquia();
        String nome = "Instituicao Listada " + UUID.randomUUID();
        pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao(nome, "L" + UUID.randomUUID().toString().substring(0, 8), modelo));

        MvcResult resultado = pedirGet(sessao, "/api/administracao/instituicoes");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains(nome);
    }
}

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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ModelosOrganizacionaisTest {

    private static final String PALAVRA_PASSE = "SenhaSegura1";
    private static final String BASE = "/api/administracao/modelos-organizacionais";
    private static final Pattern IDENTIFICADOR = Pattern.compile("\"id\":\"([0-9a-f-]{36})\"");

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private String emailAdministrador;
    private String emailComum;
    private String emailAdministradorDeInstituicao;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        emailAdministrador = "admin-" + UUID.randomUUID() + "@example.org";
        emailComum = "comum-" + UUID.randomUUID() + "@example.org";
        emailAdministradorDeInstituicao = "admin-inst-" + UUID.randomUUID() + "@example.org";
        UUID administrador = novoUtilizador(emailAdministrador);
        jdbc.update(
            "INSERT INTO atribuicao_global (utilizador_id, papel_id) SELECT CAST(? AS uuid), id FROM papel WHERE codigo = 'ADMIN_GERAL'",
            administrador);
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

    private int contar(String sql, Object... argumentos) {
        return jdbc.queryForObject(sql, Integer.class, argumentos);
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

    private UUID tipoDeUnidadeNaBase(UUID modelo, String nome, int nivel, UUID pai) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO modelo_tipo_unidade (id, modelo_id, nome, nivel, tipo_pai_id) VALUES (?, ?, ?, ?, ?)",
            id, modelo, nome, nivel, pai);
        return id;
    }

    private UUID modeloComHierarquiaNaBase(UUID tipo, String estadoDoModelo) {
        UUID modelo = modeloNaBase(tipo, estadoDoModelo);
        UUID faculdade = tipoDeUnidadeNaBase(modelo, "Faculdade", 1, null);
        UUID departamento = tipoDeUnidadeNaBase(modelo, "Departamento", 2, faculdade);
        tipoDeUnidadeNaBase(modelo, "Curso", 3, departamento);
        return modelo;
    }

    private static String tipoDeUnidade(String nome, String pai) {
        if (pai == null) {
            return "{\"nome\":\"" + nome + "\"}";
        }
        return "{\"nome\":\"" + nome + "\",\"paiNome\":\"" + pai + "\"}";
    }

    private static String hierarquiaPadrao() {
        return tipoDeUnidade("Faculdade", null) + "," + tipoDeUnidade("Departamento", "Faculdade") + ","
            + tipoDeUnidade("Curso", "Departamento");
    }

    private static String corpoModelo(UUID tipo, String nome, String tiposDeUnidade) {
        return "{\"tipoInstituicaoId\":\"" + tipo + "\",\"nome\":\"" + nome + "\",\"tiposDeUnidade\":["
            + tiposDeUnidade + "]}";
    }

    private int modelosComNome(String nome) {
        return contar("SELECT count(*) FROM modelo_organizacional WHERE lower(btrim(nome)) = lower(btrim(?))", nome);
    }

    private String estadoDoModelo(UUID modelo) {
        return jdbc.queryForObject("SELECT estado FROM modelo_organizacional WHERE id = ?", String.class, modelo);
    }

    private UUID criarModeloPelaApi(MockHttpSession sessao, UUID tipo) throws Exception {
        MvcResult resultado = pedirPost(sessao, BASE, corpoModelo(tipo, "Modelo " + UUID.randomUUID(), hierarquiaPadrao()));
        assertThat(estado(resultado)).isEqualTo(201);
        return identificador(resultado);
    }

    private String corpoInstituicao(String nome, String sigla, UUID modelo) {
        return "{\"nome\":\"" + nome + "\",\"sigla\":\"" + sigla + "\",\"modeloId\":\"" + modelo + "\"}";
    }

    @Test
    void visitanteAnonimoNaoAcedeAosModelos() throws Exception {
        MvcResult listar = mvc.perform(get(BASE)).andReturn();
        MvcResult criar = mvc.perform(post(BASE)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpoModelo(tipoNaBase("APROVADA"), "Anonimo", hierarquiaPadrao()))).andReturn();

        assertThat(estado(listar)).isEqualTo(401);
        assertThat(estado(criar)).isEqualTo(401);
        assertThat(modelosComNome("Anonimo")).isEqualTo(0);
    }

    @Test
    void utilizadorSemPapelGlobalRecebe403NosModelos() throws Exception {
        MockHttpSession sessao = entrar(emailComum);
        UUID tipo = tipoNaBase("APROVADA");
        UUID rascunho = modeloComHierarquiaNaBase(tipo, "RASCUNHO");

        assertThat(estado(pedirGet(sessao, BASE))).isEqualTo(403);
        assertThat(estado(pedirGet(sessao, BASE + "/" + rascunho))).isEqualTo(403);
        assertThat(estado(pedirPost(sessao, BASE, corpoModelo(tipo, "Sem Papel", hierarquiaPadrao())))).isEqualTo(403);
        assertThat(estado(pedirPost(sessao, BASE + "/" + rascunho + "/activar", "{}"))).isEqualTo(403);
        assertThat(estado(pedirPost(sessao, BASE + "/" + rascunho + "/inactivar", "{}"))).isEqualTo(403);
        assertThat(modelosComNome("Sem Papel")).isEqualTo(0);
        assertThat(estadoDoModelo(rascunho)).isEqualTo("RASCUNHO");
    }

    @Test
    void administradorDeInstituicaoNaoGereModelosGlobais() throws Exception {
        UUID tipo = tipoNaBase("APROVADA");
        UUID instituicao = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO instituicao (id, nome, sigla, tipo_instituicao_id, estado) VALUES (?, ?, ?, ?, 'ACTIVA')",
            instituicao, "Instituicao " + instituicao, "S" + instituicao.toString().substring(0, 8), tipo);
        UUID pessoa = novoUtilizador(emailAdministradorDeInstituicao);
        UUID vinculo = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO vinculo_institucional (id, utilizador_id, instituicao_id, tipo_vinculo, estado) VALUES (?, ?, ?, 'FUNCIONARIO', 'ACTIVO')",
            vinculo, pessoa, instituicao);
        jdbc.update(
            "INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id) SELECT CAST(? AS uuid), CAST(? AS uuid), id FROM papel WHERE codigo = 'ADMIN_INSTITUICAO'",
            vinculo, instituicao);
        MockHttpSession sessao = entrar(emailAdministradorDeInstituicao);
        MvcResult escolha = pedirPost(sessao, "/api/auth/instituicao", "{\"instituicaoId\":\"" + instituicao + "\"}");
        assertThat(estado(escolha)).isEqualTo(200);
        assertThat(estado(pedirGet(sessao, "/api/instituicao/vinculos"))).isEqualTo(200);

        MvcResult listar = pedirGet(sessao, BASE);
        MvcResult criar = pedirPost(sessao, BASE, corpoModelo(tipo, "Admin Instituicao", hierarquiaPadrao()));

        assertThat(estado(listar)).isEqualTo(403);
        assertThat(estado(criar)).isEqualTo(403);
        assertThat(modelosComNome("Admin Instituicao")).isEqualTo(0);
    }

    @Test
    void pedidoSemTokenCsrfEProibidoMesmoParaAdministrador() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = mvc.perform(post(BASE)
            .session(sessao)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpoModelo(tipoNaBase("APROVADA"), "Sem Csrf", hierarquiaPadrao()))).andReturn();

        assertThat(estado(resultado)).isEqualTo(403);
        assertThat(modelosComNome("Sem Csrf")).isEqualTo(0);
    }

    @Test
    void criarUmModeloGuardaARascunhoComAArvoreEOsNiveisDerivados() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");

        MvcResult resultado = pedirPost(sessao, BASE, corpoModelo(tipo, "Modelo Novo", hierarquiaPadrao()));

        assertThat(estado(resultado)).isEqualTo(201);
        assertThat(corpo(resultado)).contains("RASCUNHO");
        UUID modelo = identificador(resultado);
        assertThat(contar(
            "SELECT count(*) FROM modelo_organizacional WHERE id = ? AND estado = 'RASCUNHO' AND tipo_instituicao_id = ?",
            modelo, tipo)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ?", modelo)).isEqualTo(3);
        assertThat(contar(
            "SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ? AND nome = 'Faculdade' AND nivel = 1 AND tipo_pai_id IS NULL",
            modelo)).isEqualTo(1);
        assertThat(contar(
            "SELECT count(*) FROM modelo_tipo_unidade f JOIN modelo_tipo_unidade p ON p.id = f.tipo_pai_id "
                + "WHERE f.modelo_id = ? AND f.nome = 'Departamento' AND f.nivel = 2 AND p.nome = 'Faculdade'",
            modelo)).isEqualTo(1);
        assertThat(contar(
            "SELECT count(*) FROM modelo_tipo_unidade f JOIN modelo_tipo_unidade p ON p.id = f.tipo_pai_id "
                + "WHERE f.modelo_id = ? AND f.nome = 'Curso' AND f.nivel = 3 AND p.nome = 'Departamento'",
            modelo)).isEqualTo(1);
    }

    @Test
    void criarUmModeloAceitaOsTiposEmQualquerOrdem() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");
        String desordenados = tipoDeUnidade("Curso", "Departamento") + "," + tipoDeUnidade("Departamento", "Faculdade")
            + "," + tipoDeUnidade("Faculdade", null);

        MvcResult resultado = pedirPost(sessao, BASE, corpoModelo(tipo, "Modelo Desordenado", desordenados));

        assertThat(estado(resultado)).isEqualTo(201);
        UUID modelo = identificador(resultado);
        assertThat(contar(
            "SELECT count(*) FROM modelo_tipo_unidade f JOIN modelo_tipo_unidade p ON p.id = f.tipo_pai_id "
                + "WHERE f.modelo_id = ? AND f.nivel = p.nivel + 1",
            modelo)).isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ? AND nivel = 1", modelo))
            .isEqualTo(1);
    }

    @Test
    void criarUmModeloLimpaOsNomesEAsDescricao() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");
        String nome = "Modelo Limpo " + UUID.randomUUID();

        MvcResult resultado = pedirPost(sessao, BASE,
            "{\"tipoInstituicaoId\":\"" + tipo + "\",\"nome\":\"   " + nome + "   \",\"descricao\":\"  Uma descricao  \","
                + "\"tiposDeUnidade\":[{\"nome\":\"  Faculdade  \"}]}");

        assertThat(estado(resultado)).isEqualTo(201);
        UUID modelo = identificador(resultado);
        assertThat(contar(
            "SELECT count(*) FROM modelo_organizacional WHERE id = ? AND nome = ? AND descricao = 'Uma descricao'",
            modelo, nome)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ? AND nome = 'Faculdade'", modelo))
            .isEqualTo(1);
    }

    @Test
    void criarModeloComTipoDeInstituicaoInexistenteDa404() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = pedirPost(sessao, BASE, corpoModelo(UUID.randomUUID(), "Sem Tipo", hierarquiaPadrao()));

        assertThat(estado(resultado)).isEqualTo(404);
        assertThat(modelosComNome("Sem Tipo")).isEqualTo(0);
    }

    @Test
    void criarModeloComTipoDeInstituicaoNaoAprovadoDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID pendente = tipoNaBase("PENDENTE");

        MvcResult resultado = pedirPost(sessao, BASE, corpoModelo(pendente, "Tipo Pendente", hierarquiaPadrao()));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(modelosComNome("Tipo Pendente")).isEqualTo(0);
    }

    @Test
    void criarModeloComNomeRepetidoNoMesmoTipoDa409() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");
        String nome = "Modelo Repetido " + UUID.randomUUID();
        assertThat(estado(pedirPost(sessao, BASE, corpoModelo(tipo, nome, hierarquiaPadrao())))).isEqualTo(201);

        MvcResult repetido = pedirPost(sessao, BASE, corpoModelo(tipo, "  " + nome.toUpperCase() + " ", hierarquiaPadrao()));

        assertThat(estado(repetido)).isEqualTo(409);
        assertThat(contar(
            "SELECT count(*) FROM modelo_organizacional WHERE tipo_instituicao_id = ? AND lower(btrim(nome)) = lower(?)",
            tipo, nome)).isEqualTo(1);
    }

    @Test
    void omesmoNomeDeModeloEmOutroTipoDeInstituicaoEPermitido() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        String nome = "Modelo Comum " + UUID.randomUUID();
        assertThat(estado(pedirPost(sessao, BASE, corpoModelo(tipoNaBase("APROVADA"), nome, hierarquiaPadrao()))))
            .isEqualTo(201);

        MvcResult outro = pedirPost(sessao, BASE, corpoModelo(tipoNaBase("APROVADA"), nome, hierarquiaPadrao()));

        assertThat(estado(outro)).isEqualTo(201);
        assertThat(modelosComNome(nome)).isEqualTo(2);
    }

    @Test
    void arvoreComPaiInexistenteDa422SemCriarNada() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");

        MvcResult resultado = pedirPost(sessao, BASE,
            corpoModelo(tipo, "Pai Inexistente", tipoDeUnidade("Faculdade", null) + "," + tipoDeUnidade("Curso", "Escola")));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(modelosComNome("Pai Inexistente")).isEqualTo(0);
    }

    @Test
    void arvoreComCicloDa422SemCriarNada() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");

        MvcResult resultado = pedirPost(sessao, BASE,
            corpoModelo(tipo, "Com Ciclo", tipoDeUnidade("Raiz", null) + "," + tipoDeUnidade("A", "B") + ","
                + tipoDeUnidade("B", "A")));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(modelosComNome("Com Ciclo")).isEqualTo(0);
    }

    @Test
    void arvoreComNomesRepetidosDa422SemCriarNada() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");

        MvcResult resultado = pedirPost(sessao, BASE,
            corpoModelo(tipo, "Nomes Repetidos", tipoDeUnidade("Faculdade", null) + "," + tipoDeUnidade("FACULDADE ", null)));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(modelosComNome("Nomes Repetidos")).isEqualTo(0);
    }

    @Test
    void corpoSemTiposDeUnidadeDa400() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");

        MvcResult resultado = pedirPost(sessao, BASE,
            "{\"tipoInstituicaoId\":\"" + tipo + "\",\"nome\":\"Sem Tipos\",\"tiposDeUnidade\":[]}");

        assertThat(estado(resultado)).isEqualTo(400);
        assertThat(modelosComNome("Sem Tipos")).isEqualTo(0);
    }

    @Test
    void corpoComElementoNuloNaListaDa400() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");

        MvcResult resultado = pedirPost(sessao, BASE,
            "{\"tipoInstituicaoId\":\"" + tipo + "\",\"nome\":\"Elemento Nulo\",\"tiposDeUnidade\":[null]}");

        assertThat(estado(resultado)).isEqualTo(400);
        assertThat(modelosComNome("Elemento Nulo")).isEqualTo(0);
    }

    @Test
    void corpoComNomeEmBrancoDa400() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");

        MvcResult resultado = pedirPost(sessao, BASE, corpoModelo(tipo, "   ", hierarquiaPadrao()));

        assertThat(estado(resultado)).isEqualTo(400);
        assertThat(contar("SELECT count(*) FROM modelo_organizacional WHERE tipo_instituicao_id = ?", tipo)).isEqualTo(0);
    }

    @Test
    void listarDevolveOsModelosEFiltraPorEstado() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID tipo = tipoNaBase("APROVADA");
        UUID rascunho = modeloComHierarquiaNaBase(tipo, "RASCUNHO");
        UUID activo = modeloComHierarquiaNaBase(tipo, "ACTIVO");

        MvcResult todos = pedirGet(sessao, BASE);
        MvcResult soActivos = pedirGet(sessao, BASE + "?estado=ACTIVO");

        assertThat(estado(todos)).isEqualTo(200);
        assertThat(corpo(todos)).contains(rascunho.toString()).contains(activo.toString());
        assertThat(corpo(todos)).contains("\"totalDeTiposDeUnidade\":3");
        assertThat(estado(soActivos)).isEqualTo(200);
        assertThat(corpo(soActivos)).contains(activo.toString()).doesNotContain(rascunho.toString());
    }

    @Test
    void listarComEstadoInvalidoDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = pedirGet(sessao, BASE + "?estado=PUBLICADO");

        assertThat(estado(resultado)).isEqualTo(422);
    }

    @Test
    void obterDevolveOModeloComOsSeusTiposDeUnidade() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloComHierarquiaNaBase(tipoNaBase("APROVADA"), "RASCUNHO");

        MvcResult resultado = pedirGet(sessao, BASE + "/" + modelo);

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains(modelo.toString()).contains("Faculdade").contains("Departamento")
            .contains("Curso").contains("\"nivel\":3");
    }

    @Test
    void obterUmModeloInexistenteDa404() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = pedirGet(sessao, BASE + "/" + UUID.randomUUID());

        assertThat(estado(resultado)).isEqualTo(404);
    }

    @Test
    void activarUmModeloEmRascunho() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloComHierarquiaNaBase(tipoNaBase("APROVADA"), "RASCUNHO");

        MvcResult resultado = pedirPost(sessao, BASE + "/" + modelo + "/activar", "{}");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains("ACTIVO");
        assertThat(estadoDoModelo(modelo)).isEqualTo("ACTIVO");
    }

    @Test
    void activarUmModeloJaActivoDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloComHierarquiaNaBase(tipoNaBase("APROVADA"), "ACTIVO");

        MvcResult resultado = pedirPost(sessao, BASE + "/" + modelo + "/activar", "{}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(estadoDoModelo(modelo)).isEqualTo("ACTIVO");
    }

    @Test
    void activarUmModeloSemTiposDeUnidadeDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloNaBase(tipoNaBase("APROVADA"), "RASCUNHO");

        MvcResult resultado = pedirPost(sessao, BASE + "/" + modelo + "/activar", "{}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(estadoDoModelo(modelo)).isEqualTo("RASCUNHO");
    }

    @Test
    void activarUmModeloComNivelInconsistenteDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloNaBase(tipoNaBase("APROVADA"), "RASCUNHO");
        UUID faculdade = tipoDeUnidadeNaBase(modelo, "Faculdade", 1, null);
        UUID departamento = tipoDeUnidadeNaBase(modelo, "Departamento", 2, faculdade);
        tipoDeUnidadeNaBase(modelo, "Curso", 4, departamento);

        MvcResult resultado = pedirPost(sessao, BASE + "/" + modelo + "/activar", "{}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(estadoDoModelo(modelo)).isEqualTo("RASCUNHO");
    }

    @Test
    void activarUmModeloComTipoDeInstituicaoNaoAprovadoDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloComHierarquiaNaBase(tipoNaBase("PENDENTE"), "RASCUNHO");

        MvcResult resultado = pedirPost(sessao, BASE + "/" + modelo + "/activar", "{}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(estadoDoModelo(modelo)).isEqualTo("RASCUNHO");
    }

    @Test
    void activarUmModeloInexistenteDa404() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = pedirPost(sessao, BASE + "/" + UUID.randomUUID() + "/activar", "{}");

        assertThat(estado(resultado)).isEqualTo(404);
    }

    @Test
    void inactivarUmModeloActivo() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloComHierarquiaNaBase(tipoNaBase("APROVADA"), "ACTIVO");

        MvcResult resultado = pedirPost(sessao, BASE + "/" + modelo + "/inactivar", "{}");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(estadoDoModelo(modelo)).isEqualTo("INACTIVO");
    }

    @Test
    void inactivarUmModeloEmRascunhoDa422() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloComHierarquiaNaBase(tipoNaBase("APROVADA"), "RASCUNHO");

        MvcResult resultado = pedirPost(sessao, BASE + "/" + modelo + "/inactivar", "{}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(estadoDoModelo(modelo)).isEqualTo("RASCUNHO");
    }

    @Test
    void inactivarUmModeloInexistenteDa404() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);

        MvcResult resultado = pedirPost(sessao, BASE + "/" + UUID.randomUUID() + "/inactivar", "{}");

        assertThat(estado(resultado)).isEqualTo(404);
    }

    @Test
    void umModeloInactivoPodeSerReactivado() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = modeloComHierarquiaNaBase(tipoNaBase("APROVADA"), "INACTIVO");

        MvcResult resultado = pedirPost(sessao, BASE + "/" + modelo + "/activar", "{}");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(estadoDoModelo(modelo)).isEqualTo("ACTIVO");
    }

    @Test
    void cicloCompletoTipoModeloEInstituicaoSoPelaApi() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        String nomeDoTipo = "Tipo Do Ciclo " + UUID.randomUUID();
        MvcResult proposta = pedirPost(sessao, "/api/administracao/tipos-instituicao", "{\"nome\":\"" + nomeDoTipo + "\"}");
        assertThat(estado(proposta)).isEqualTo(201);
        UUID tipo = identificador(proposta);
        assertThat(estado(pedirPost(sessao, "/api/administracao/tipos-instituicao/" + tipo + "/aprovar", "{}")))
            .isEqualTo(200);
        UUID modelo = criarModeloPelaApi(sessao, tipo);
        assertThat(estado(pedirPost(sessao, BASE + "/" + modelo + "/activar", "{}"))).isEqualTo(200);
        String nomeDaInstituicao = "Instituicao Do Ciclo " + UUID.randomUUID();

        MvcResult criada = pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao(nomeDaInstituicao, "C" + UUID.randomUUID().toString().substring(0, 8), modelo));

        assertThat(estado(criada)).isEqualTo(201);
        UUID instituicao = identificador(criada);
        assertThat(contar(
            "SELECT count(*) FROM instituicao WHERE id = ? AND modelo_origem_id = ? AND tipo_instituicao_id = ? AND estado = 'ACTIVA'",
            instituicao, modelo, tipo)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM tipo_unidade WHERE instituicao_id = ?", instituicao)).isEqualTo(3);
        assertThat(contar(
            "SELECT count(*) FROM tipo_unidade f JOIN tipo_unidade p ON p.id = f.tipo_pai_id "
                + "WHERE f.instituicao_id = ? AND f.nivel = p.nivel + 1",
            instituicao)).isEqualTo(2);
    }

    @Test
    void modeloEmRascunhoNaoServeParaCriarInstituicoes() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloPelaApi(sessao, tipoNaBase("APROVADA"));
        String nome = "Instituicao Rascunho " + UUID.randomUUID();

        MvcResult resultado = pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao(nome, "R" + UUID.randomUUID().toString().substring(0, 8), modelo));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM instituicao WHERE nome = ?", nome)).isEqualTo(0);
    }

    @Test
    void inactivarUmModeloImpedeNovasInstituicoesMasNaoAfectaAsExistentes() throws Exception {
        MockHttpSession sessao = entrar(emailAdministrador);
        UUID modelo = criarModeloPelaApi(sessao, tipoNaBase("APROVADA"));
        assertThat(estado(pedirPost(sessao, BASE + "/" + modelo + "/activar", "{}"))).isEqualTo(200);
        MvcResult primeira = pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao("Primeira " + UUID.randomUUID(), "P" + UUID.randomUUID().toString().substring(0, 8), modelo));
        assertThat(estado(primeira)).isEqualTo(201);
        UUID existente = identificador(primeira);
        assertThat(estado(pedirPost(sessao, BASE + "/" + modelo + "/inactivar", "{}"))).isEqualTo(200);
        String nomeDaSegunda = "Segunda " + UUID.randomUUID();

        MvcResult segunda = pedirPost(sessao, "/api/administracao/instituicoes",
            corpoInstituicao(nomeDaSegunda, "S" + UUID.randomUUID().toString().substring(0, 8), modelo));

        assertThat(estado(segunda)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM instituicao WHERE nome = ?", nomeDaSegunda)).isEqualTo(0);
        assertThat(contar("SELECT count(*) FROM tipo_unidade WHERE instituicao_id = ?", existente)).isEqualTo(3);
        assertThat(contar("SELECT count(*) FROM instituicao WHERE id = ? AND estado = 'ACTIVA'", existente)).isEqualTo(1);
    }
}

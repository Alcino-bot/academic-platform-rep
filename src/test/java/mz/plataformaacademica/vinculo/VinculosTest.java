package mz.plataformaacademica.vinculo;

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
class VinculosTest {

    private static final String PALAVRA_PASSE = "SenhaSegura1";
    private static final Pattern IDENTIFICADOR = Pattern.compile("\"id\":\"([0-9a-f-]{36})\"");

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private UUID tipoId;
    private UUID instituicaoA;
    private UUID instituicaoB;
    private String emailAdministradorA;
    private UUID administradorA;
    private String emailPessoa;
    private UUID pessoa;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        tipoId = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO tipo_instituicao (id, nome, estado, data_aprovacao) VALUES (?, ?, 'APROVADA', now())",
            tipoId, "Tipo " + tipoId);
        instituicaoA = novaInstituicao("ACTIVA");
        instituicaoB = novaInstituicao("ACTIVA");
        emailAdministradorA = "admin-" + UUID.randomUUID() + "@example.org";
        administradorA = novoUtilizador(emailAdministradorA);
        UUID vinculoAdministrador = novoVinculo(administradorA, instituicaoA, "FUNCIONARIO", "ACTIVO");
        darPapel(vinculoAdministrador, instituicaoA, "ADMIN_INSTITUICAO");
        emailPessoa = "pessoa-" + UUID.randomUUID() + "@example.org";
        pessoa = novoUtilizador(emailPessoa);
    }

    private UUID novaInstituicao(String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO instituicao (id, nome, sigla, tipo_instituicao_id, estado) VALUES (?, ?, ?, ?, ?)",
            id, "Instituicao " + id, "S" + id.toString().substring(0, 8), tipoId, estado);
        return id;
    }

    private UUID novoUtilizador(String email) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO utilizador (id, email, nome_completo, palavra_passe_hash, estado) VALUES (?, ?, 'Pessoa Teste', ?, 'ACTIVO')",
            id, email, new BCryptPasswordEncoder().encode(PALAVRA_PASSE));
        return id;
    }

    private UUID novoVinculo(UUID utilizador, UUID instituicao, String tipo, String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO vinculo_institucional (id, utilizador_id, instituicao_id, tipo_vinculo, estado) VALUES (?, ?, ?, ?, ?)",
            id, utilizador, instituicao, tipo, estado);
        return id;
    }

    private void darPapel(UUID vinculo, UUID instituicao, String papel) {
        jdbc.update(
            "INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id) SELECT CAST(? AS uuid), CAST(? AS uuid), id FROM papel WHERE codigo = ?",
            vinculo, instituicao, papel);
    }

    private MockHttpSession entrar(String email) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/entrar")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"palavraPasse\":\"" + PALAVRA_PASSE + "\"}")).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    private MockHttpSession entrarNaInstituicao(String email, UUID instituicao) throws Exception {
        MockHttpSession sessao = entrar(email);
        MvcResult escolha = pedirPost(sessao, "/api/auth/instituicao", "{\"instituicaoId\":\"" + instituicao + "\"}");
        assertThat(escolha.getResponse().getStatus()).isEqualTo(200);
        return sessao;
    }

    private MockHttpSession administradorDaA() throws Exception {
        return entrarNaInstituicao(emailAdministradorA, instituicaoA);
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

    private String pedido(UUID instituicao, String tipo) {
        return "{\"instituicaoId\":\"" + instituicao + "\",\"tipoVinculo\":\"" + tipo + "\"}";
    }

    private int papeisActivos(UUID vinculo, String papel) {
        return contar(
            "SELECT count(*) FROM vinculo_papel vp JOIN papel p ON p.id = vp.papel_id WHERE vp.vinculo_id = ? AND p.codigo = ? AND vp.estado = 'ACTIVO'",
            vinculo, papel);
    }

    @Test
    void pedidoDeVinculoSemSessaoRecebe401() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/vinculos/pedidos")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(pedido(instituicaoA, "ESTUDANTE"))).andReturn();

        assertThat(estado(resultado)).isEqualTo(401);
    }

    @Test
    void utilizadorPedeUmVinculoEFicaPendente() throws Exception {
        MockHttpSession sessao = entrar(emailPessoa);

        MvcResult resultado = pedirPost(sessao, "/api/vinculos/pedidos", pedido(instituicaoA, "ESTUDANTE"));

        assertThat(estado(resultado)).isEqualTo(201);
        assertThat(corpo(resultado)).contains("PENDENTE");
        assertThat(contar(
            "SELECT count(*) FROM vinculo_institucional WHERE utilizador_id = ? AND instituicao_id = ? AND tipo_vinculo = 'ESTUDANTE' AND estado = 'PENDENTE'",
            pessoa, instituicaoA)).isEqualTo(1);
    }

    @Test
    void pedidoDeTipoInvalidoDa422() throws Exception {
        MockHttpSession sessao = entrar(emailPessoa);

        MvcResult resultado = pedirPost(sessao, "/api/vinculos/pedidos", pedido(instituicaoA, "ALUNO"));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE utilizador_id = ?", pessoa)).isEqualTo(0);
    }

    @Test
    void pedidoDeInstituicaoInexistenteDa404() throws Exception {
        MockHttpSession sessao = entrar(emailPessoa);

        MvcResult resultado = pedirPost(sessao, "/api/vinculos/pedidos", pedido(UUID.randomUUID(), "ESTUDANTE"));

        assertThat(estado(resultado)).isEqualTo(404);
    }

    @Test
    void pedidoDeInstituicaoNaoActivaDa422() throws Exception {
        UUID suspensa = novaInstituicao("SUSPENSA");
        MockHttpSession sessao = entrar(emailPessoa);

        MvcResult resultado = pedirPost(sessao, "/api/vinculos/pedidos", pedido(suspensa, "ESTUDANTE"));

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE instituicao_id = ?", suspensa)).isEqualTo(0);
    }

    @Test
    void pedidoRepetidoDa409EnaoDuplica() throws Exception {
        MockHttpSession sessao = entrar(emailPessoa);
        pedirPost(sessao, "/api/vinculos/pedidos", pedido(instituicaoA, "ESTUDANTE"));

        MvcResult repetido = pedirPost(sessao, "/api/vinculos/pedidos", pedido(instituicaoA, "ESTUDANTE"));

        assertThat(estado(repetido)).isEqualTo(409);
        assertThat(contar(
            "SELECT count(*) FROM vinculo_institucional WHERE utilizador_id = ? AND instituicao_id = ?",
            pessoa, instituicaoA)).isEqualTo(1);
    }

    @Test
    void omesmoUtilizadorPodePedirTiposDiferentesEInstituicoesDiferentes() throws Exception {
        MockHttpSession sessao = entrar(emailPessoa);

        int a = estado(pedirPost(sessao, "/api/vinculos/pedidos", pedido(instituicaoA, "ESTUDANTE")));
        int b = estado(pedirPost(sessao, "/api/vinculos/pedidos", pedido(instituicaoA, "OUTRO")));
        int c = estado(pedirPost(sessao, "/api/vinculos/pedidos", pedido(instituicaoB, "ESTUDANTE")));

        assertThat(a).isEqualTo(201);
        assertThat(b).isEqualTo(201);
        assertThat(c).isEqualTo(201);
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE utilizador_id = ?", pessoa)).isEqualTo(3);
    }

    @Test
    void meusVinculosListaSoOsDoProprioUtilizador() throws Exception {
        UUID outra = novoUtilizador("outra-" + UUID.randomUUID() + "@example.org");
        UUID vinculoDaOutra = novoVinculo(outra, instituicaoA, "DOCENTE", "PENDENTE");
        MockHttpSession sessao = entrar(emailPessoa);
        UUID meu = identificador(pedirPost(sessao, "/api/vinculos/pedidos", pedido(instituicaoA, "ESTUDANTE")));

        MvcResult resultado = pedirGet(sessao, "/api/vinculos/meus");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains(meu.toString());
        assertThat(corpo(resultado)).doesNotContain(vinculoDaOutra.toString());
    }

    @Test
    void utilizadorSemPapelDeAdministradorRecebe403() throws Exception {
        novoVinculo(pessoa, instituicaoA, "ESTUDANTE", "ACTIVO");
        MockHttpSession sessao = entrarNaInstituicao(emailPessoa, instituicaoA);

        assertThat(estado(pedirGet(sessao, "/api/instituicao/vinculos"))).isEqualTo(403);
    }

    @Test
    void administradorSemInstituicaoEscolhidaRecebe403() throws Exception {
        MockHttpSession sessao = entrar(emailAdministradorA);

        assertThat(estado(pedirGet(sessao, "/api/instituicao/vinculos"))).isEqualTo(403);
    }

    @Test
    void administradorListaSoOsPedidosDaSuaInstituicao() throws Exception {
        UUID naA = novoVinculo(pessoa, instituicaoA, "ESTUDANTE", "PENDENTE");
        UUID outra = novoUtilizador("outra-" + UUID.randomUUID() + "@example.org");
        UUID naB = novoVinculo(outra, instituicaoB, "ESTUDANTE", "PENDENTE");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirGet(sessao, "/api/instituicao/vinculos");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains(naA.toString());
        assertThat(corpo(resultado)).doesNotContain(naB.toString());
    }

    @Test
    void listaFiltraPorEstadoERecusaEstadoInvalido() throws Exception {
        UUID pendente = novoVinculo(pessoa, instituicaoA, "ESTUDANTE", "PENDENTE");
        UUID outra = novoUtilizador("outra-" + UUID.randomUUID() + "@example.org");
        UUID activo = novoVinculo(outra, instituicaoA, "DOCENTE", "ACTIVO");
        MockHttpSession sessao = administradorDaA();

        MvcResult soPendentes = pedirGet(sessao, "/api/instituicao/vinculos?estado=PENDENTE");
        MvcResult invalido = pedirGet(sessao, "/api/instituicao/vinculos?estado=INVALIDO");

        assertThat(corpo(soPendentes)).contains(pendente.toString());
        assertThat(corpo(soPendentes)).doesNotContain(activo.toString());
        assertThat(estado(invalido)).isEqualTo(422);
    }

    @Test
    void aprovarUmPedidoDeEstudanteActivaOVinculoEDaOPapelEstudante() throws Exception {
        UUID vinculo = novoVinculo(pessoa, instituicaoA, "ESTUDANTE", "PENDENTE");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/aprovar", "{}");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(contar(
            "SELECT count(*) FROM vinculo_institucional WHERE id = ? AND estado = 'ACTIVO' AND decidido_por = ? AND data_decisao IS NOT NULL",
            vinculo, administradorA)).isEqualTo(1);
        assertThat(papeisActivos(vinculo, "ESTUDANTE")).isEqualTo(1);
    }

    @Test
    void aprovarUmPedidoDeDocenteDaOPapelDocente() throws Exception {
        UUID vinculo = novoVinculo(pessoa, instituicaoA, "DOCENTE", "PENDENTE");
        MockHttpSession sessao = administradorDaA();

        pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/aprovar", "{}");

        assertThat(papeisActivos(vinculo, "DOCENTE")).isEqualTo(1);
    }

    @Test
    void aprovarUmPedidoDeFuncionarioNaoDaPapelNenhum() throws Exception {
        UUID vinculo = novoVinculo(pessoa, instituicaoA, "FUNCIONARIO", "PENDENTE");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/aprovar", "{}");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(contar("SELECT count(*) FROM vinculo_papel WHERE vinculo_id = ?", vinculo)).isEqualTo(0);
    }

    @Test
    void rejeitarUmPedidoGuardaOMotivoEQuemDecidiu() throws Exception {
        UUID vinculo = novoVinculo(pessoa, instituicaoA, "ESTUDANTE", "PENDENTE");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/rejeitar",
            "{\"motivo\":\"Documentos em falta\"}");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains("REJEITADO");
        assertThat(contar(
            "SELECT count(*) FROM vinculo_institucional WHERE id = ? AND estado = 'REJEITADO' AND decidido_por = ? AND motivo_rejeicao = 'Documentos em falta'",
            vinculo, administradorA)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM vinculo_papel WHERE vinculo_id = ?", vinculo)).isEqualTo(0);
    }

    @Test
    void rejeitarSemMotivoDa400() throws Exception {
        UUID vinculo = novoVinculo(pessoa, instituicaoA, "ESTUDANTE", "PENDENTE");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/rejeitar",
            "{\"motivo\":\"   \"}");

        assertThat(estado(resultado)).isEqualTo(400);
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE id = ? AND estado = 'PENDENTE'", vinculo))
            .isEqualTo(1);
    }

    @Test
    void umPedidoJaDecididoNaoMudaDeEstado() throws Exception {
        UUID vinculo = novoVinculo(pessoa, instituicaoA, "ESTUDANTE", "PENDENTE");
        MockHttpSession sessao = administradorDaA();
        pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/aprovar", "{}");

        MvcResult aprovarOutraVez = pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/aprovar", "{}");
        MvcResult rejeitarDepois = pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/rejeitar",
            "{\"motivo\":\"Tarde demais\"}");

        assertThat(estado(aprovarOutraVez)).isEqualTo(422);
        assertThat(estado(rejeitarDepois)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE id = ? AND estado = 'ACTIVO'", vinculo))
            .isEqualTo(1);
    }

    @Test
    void ninguemDecideOProprioPedido() throws Exception {
        UUID proprio = novoVinculo(administradorA, instituicaoA, "OUTRO", "PENDENTE");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirPost(sessao, "/api/instituicao/vinculos/" + proprio + "/aprovar", "{}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE id = ? AND estado = 'PENDENTE'", proprio))
            .isEqualTo(1);
    }

    @Test
    void administradorNaoDecidePedidosDeOutraInstituicao() throws Exception {
        UUID naB = novoVinculo(pessoa, instituicaoB, "ESTUDANTE", "PENDENTE");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirPost(sessao, "/api/instituicao/vinculos/" + naB + "/aprovar", "{}");

        assertThat(estado(resultado)).isEqualTo(404);
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE id = ? AND estado = 'PENDENTE'", naB))
            .isEqualTo(1);
    }

    @Test
    void depoisDeAprovadaAPessoaEntraNaInstituicaoComOsPapeis() throws Exception {
        MockHttpSession sessaoPessoa = entrar(emailPessoa);
        UUID vinculo = identificador(pedirPost(sessaoPessoa, "/api/vinculos/pedidos", pedido(instituicaoA, "ESTUDANTE")));
        pedirPost(administradorDaA(), "/api/instituicao/vinculos/" + vinculo + "/aprovar", "{}");

        MvcResult escolha = pedirPost(sessaoPessoa, "/api/auth/instituicao", "{\"instituicaoId\":\"" + instituicaoA + "\"}");

        assertThat(estado(escolha)).isEqualTo(200);
        assertThat(corpo(escolha)).contains("PAPEL_ESTUDANTE").contains("VINCULO_ESTUDANTE");
    }

    @Test
    void umPedidoPendenteNaoDaAcessoAInstituicao() throws Exception {
        MockHttpSession sessao = entrar(emailPessoa);
        pedirPost(sessao, "/api/vinculos/pedidos", pedido(instituicaoA, "ESTUDANTE"));

        MvcResult escolha = pedirPost(sessao, "/api/auth/instituicao", "{\"instituicaoId\":\"" + instituicaoA + "\"}");

        assertThat(estado(escolha)).isNotEqualTo(200);
    }

    @Test
    void depoisDeRejeitadaAPessoaPodePedirDeNovo() throws Exception {
        MockHttpSession sessaoPessoa = entrar(emailPessoa);
        UUID vinculo = identificador(pedirPost(sessaoPessoa, "/api/vinculos/pedidos", pedido(instituicaoA, "ESTUDANTE")));
        pedirPost(administradorDaA(), "/api/instituicao/vinculos/" + vinculo + "/rejeitar", "{\"motivo\":\"Sem comprovativo\"}");

        MvcResult novo = pedirPost(sessaoPessoa, "/api/vinculos/pedidos", pedido(instituicaoA, "ESTUDANTE"));

        assertThat(estado(novo)).isEqualTo(201);
        assertThat(contar(
            "SELECT count(*) FROM vinculo_institucional WHERE id = ? AND estado = 'PENDENTE' AND decidido_por IS NULL AND motivo_rejeicao IS NULL",
            vinculo)).isEqualTo(1);
    }

    @Test
    void atribuiOPapelDeRevisorAUmVinculoActivo() throws Exception {
        UUID vinculo = novoVinculo(pessoa, instituicaoA, "DOCENTE", "ACTIVO");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/papeis",
            "{\"papel\":\"REVISOR_BIBLIOTECA\"}");

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains("REVISOR_BIBLIOTECA");
        assertThat(papeisActivos(vinculo, "REVISOR_BIBLIOTECA")).isEqualTo(1);
    }

    @Test
    void oAdministradorDaInstituicaoNaoAtribuiOPapelDeAdministrador() throws Exception {
        UUID vinculo = novoVinculo(pessoa, instituicaoA, "FUNCIONARIO", "ACTIVO");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/papeis",
            "{\"papel\":\"ADMIN_INSTITUICAO\"}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(papeisActivos(vinculo, "ADMIN_INSTITUICAO")).isEqualTo(0);
    }

    @Test
    void naoSeAtribuiPapelAUmVinculoQueNaoEstaActivo() throws Exception {
        UUID vinculo = novoVinculo(pessoa, instituicaoA, "DOCENTE", "PENDENTE");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/papeis",
            "{\"papel\":\"REVISOR_BIBLIOTECA\"}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM vinculo_papel WHERE vinculo_id = ?", vinculo)).isEqualTo(0);
    }

    @Test
    void opapelDocenteSoVaiParaVinculosDocentes() throws Exception {
        UUID vinculo = novoVinculo(pessoa, instituicaoA, "ESTUDANTE", "ACTIVO");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirPost(sessao, "/api/instituicao/vinculos/" + vinculo + "/papeis",
            "{\"papel\":\"DOCENTE\"}");

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(papeisActivos(vinculo, "DOCENTE")).isEqualTo(0);
    }

    @Test
    void naoSeAtribuiPapelAUmVinculoDeOutraInstituicao() throws Exception {
        UUID naB = novoVinculo(pessoa, instituicaoB, "DOCENTE", "ACTIVO");
        MockHttpSession sessao = administradorDaA();

        MvcResult resultado = pedirPost(sessao, "/api/instituicao/vinculos/" + naB + "/papeis",
            "{\"papel\":\"REVISOR_BIBLIOTECA\"}");

        assertThat(estado(resultado)).isEqualTo(404);
        assertThat(contar("SELECT count(*) FROM vinculo_papel WHERE vinculo_id = ?", naB)).isEqualTo(0);
    }

    @Test
    void revogaUmPapelEDepoisJaNaoOTem() throws Exception {
        UUID vinculo = novoVinculo(pessoa, instituicaoA, "DOCENTE", "ACTIVO");
        darPapel(vinculo, instituicaoA, "REVISOR_BIBLIOTECA");
        MockHttpSession sessao = administradorDaA();

        MvcResult revogado = pedirPost(sessao,
            "/api/instituicao/vinculos/" + vinculo + "/papeis/REVISOR_BIBLIOTECA/revogar", "{}");
        MvcResult outraVez = pedirPost(sessao,
            "/api/instituicao/vinculos/" + vinculo + "/papeis/REVISOR_BIBLIOTECA/revogar", "{}");

        assertThat(estado(revogado)).isEqualTo(200);
        assertThat(estado(outraVez)).isEqualTo(404);
        assertThat(papeisActivos(vinculo, "REVISOR_BIBLIOTECA")).isEqualTo(0);
    }
}

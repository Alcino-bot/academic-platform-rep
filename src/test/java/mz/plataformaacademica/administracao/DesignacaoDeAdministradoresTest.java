package mz.plataformaacademica.administracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
class DesignacaoDeAdministradoresTest {

    private static final String PALAVRA_PASSE = "SenhaSegura1";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private UUID tipoId;
    private UUID instituicaoA;
    private UUID instituicaoB;
    private String emailGeral;
    private UUID geral;
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
        emailGeral = "geral-" + UUID.randomUUID() + "@example.org";
        geral = novoUtilizador(emailGeral, "ACTIVO");
        jdbc.update(
            "INSERT INTO atribuicao_global (utilizador_id, papel_id) SELECT CAST(? AS uuid), id FROM papel WHERE codigo = 'ADMIN_GERAL'",
            geral);
        emailPessoa = "pessoa-" + UUID.randomUUID() + "@example.org";
        pessoa = novoUtilizador(emailPessoa, "ACTIVO");
    }

    private UUID novaInstituicao(String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO instituicao (id, nome, sigla, tipo_instituicao_id, estado) VALUES (?, ?, ?, ?, ?)",
            id, "Instituicao " + id, "S" + id.toString().substring(0, 8), tipoId, estado);
        return id;
    }

    private UUID novoUtilizador(String email, String estadoDaConta) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO utilizador (id, email, nome_completo, palavra_passe_hash, estado) VALUES (?, ?, 'Pessoa Teste', ?, ?)",
            id, email, new BCryptPasswordEncoder().encode(PALAVRA_PASSE), estadoDaConta);
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

    private void escolherInstituicao(MockHttpSession sessao, UUID instituicao) throws Exception {
        mvc.perform(post("/api/auth/instituicao")
            .session(sessao)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"instituicaoId\":\"" + instituicao + "\"}")).andReturn();
    }

    private MvcResult designar(MockHttpSession sessao, UUID instituicao, String email) throws Exception {
        return mvc.perform(post("/api/administracao/instituicoes/" + instituicao + "/administradores")
            .session(sessao)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\"}")).andReturn();
    }

    private int estado(MvcResult resultado) {
        return resultado.getResponse().getStatus();
    }

    private String corpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private int contar(String sql, Object... argumentos) {
        return jdbc.queryForObject(sql, Integer.class, argumentos);
    }

    private int administradoresDe(UUID instituicao, UUID utilizador) {
        return contar(
            "SELECT count(*) FROM vinculo_papel vp JOIN papel p ON p.id = vp.papel_id JOIN vinculo_institucional v ON v.id = vp.vinculo_id AND v.instituicao_id = vp.instituicao_id WHERE vp.instituicao_id = ? AND v.utilizador_id = ? AND p.codigo = 'ADMIN_INSTITUICAO' AND vp.estado = 'ACTIVO'",
            instituicao, utilizador);
    }

    @Test
    void designarCriaUmVinculoDeFuncionarioActivoEOPapelDeAdministrador() throws Exception {
        MvcResult resultado = designar(entrar(emailGeral), instituicaoA, emailPessoa);

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"jaEraAdministrador\":false");
        assertThat(contar(
            "SELECT count(*) FROM vinculo_institucional WHERE utilizador_id = ? AND instituicao_id = ? AND tipo_vinculo = 'FUNCIONARIO' AND estado = 'ACTIVO' AND decidido_por = ?",
            pessoa, instituicaoA, geral)).isEqualTo(1);
        assertThat(administradoresDe(instituicaoA, pessoa)).isEqualTo(1);
    }

    @Test
    void oAdministradorDesignadoEntraEAcedeAAdministracaoDaSuaInstituicao() throws Exception {
        designar(entrar(emailGeral), instituicaoA, emailPessoa);
        MockHttpSession sessaoPessoa = entrar(emailPessoa);
        escolherInstituicao(sessaoPessoa, instituicaoA);

        MvcResult lista = mvc.perform(get("/api/instituicao/vinculos").session(sessaoPessoa)).andReturn();

        assertThat(estado(lista)).isEqualTo(200);
    }

    @Test
    void designarReutilizaUmVinculoActivoQueJaExiste() throws Exception {
        UUID docente = novoVinculo(pessoa, instituicaoA, "DOCENTE", "ACTIVO");

        MvcResult resultado = designar(entrar(emailGeral), instituicaoA, emailPessoa);

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(corpo(resultado)).contains(docente.toString());
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE utilizador_id = ? AND instituicao_id = ?",
            pessoa, instituicaoA)).isEqualTo(1);
        assertThat(administradoresDe(instituicaoA, pessoa)).isEqualTo(1);
    }

    @Test
    void designarUmPedidoPendenteDeFuncionarioActivaOMesmoVinculo() throws Exception {
        UUID pendente = novoVinculo(pessoa, instituicaoA, "FUNCIONARIO", "PENDENTE");

        MvcResult resultado = designar(entrar(emailGeral), instituicaoA, emailPessoa);

        assertThat(estado(resultado)).isEqualTo(200);
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE id = ? AND estado = 'ACTIVO'", pendente))
            .isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE utilizador_id = ? AND instituicao_id = ?",
            pessoa, instituicaoA)).isEqualTo(1);
    }

    @Test
    void designarDuasVezesEIdempotente() throws Exception {
        MockHttpSession sessao = entrar(emailGeral);
        designar(sessao, instituicaoA, emailPessoa);

        MvcResult segunda = designar(sessao, instituicaoA, emailPessoa);

        assertThat(estado(segunda)).isEqualTo(200);
        assertThat(corpo(segunda)).contains("\"jaEraAdministrador\":true");
        assertThat(administradoresDe(instituicaoA, pessoa)).isEqualTo(1);
    }

    @Test
    void emailSemContaActivaDa404() throws Exception {
        String suspenso = "suspenso-" + UUID.randomUUID() + "@example.org";
        novoUtilizador(suspenso, "SUSPENSO");
        MockHttpSession sessao = entrar(emailGeral);

        MvcResult inexistente = designar(sessao, instituicaoA, "nao-existe-" + UUID.randomUUID() + "@example.org");
        MvcResult contaSuspensa = designar(sessao, instituicaoA, suspenso);

        assertThat(estado(inexistente)).isEqualTo(404);
        assertThat(estado(contaSuspensa)).isEqualTo(404);
    }

    @Test
    void instituicaoNaoActivaDa422ENaoCriaNada() throws Exception {
        UUID suspensa = novaInstituicao("SUSPENSA");

        MvcResult resultado = designar(entrar(emailGeral), suspensa, emailPessoa);

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE instituicao_id = ?", suspensa)).isEqualTo(0);
    }

    @Test
    void instituicaoInexistenteDa404() throws Exception {
        MvcResult resultado = designar(entrar(emailGeral), UUID.randomUUID(), emailPessoa);

        assertThat(estado(resultado)).isEqualTo(404);
    }

    @Test
    void vinculoDeFuncionarioSuspensoDa422() throws Exception {
        novoVinculo(pessoa, instituicaoA, "FUNCIONARIO", "SUSPENSO");

        MvcResult resultado = designar(entrar(emailGeral), instituicaoA, emailPessoa);

        assertThat(estado(resultado)).isEqualTo(422);
        assertThat(administradoresDe(instituicaoA, pessoa)).isEqualTo(0);
    }

    @Test
    void soOAdministradorGeralDesigna() throws Exception {
        UUID vinculoAdmin = novoVinculo(pessoa, instituicaoA, "FUNCIONARIO", "ACTIVO");
        darPapel(vinculoAdmin, instituicaoA, "ADMIN_INSTITUICAO");
        String emailOutra = "outra-" + UUID.randomUUID() + "@example.org";
        UUID outra = novoUtilizador(emailOutra, "ACTIVO");
        MockHttpSession comum = entrar(emailOutra);
        MockHttpSession administradorDaInstituicao = entrar(emailPessoa);
        escolherInstituicao(administradorDaInstituicao, instituicaoA);

        MvcResult peloComum = designar(comum, instituicaoA, emailOutra);
        MvcResult peloAdministradorDaInstituicao = designar(administradorDaInstituicao, instituicaoA, emailOutra);

        assertThat(estado(peloComum)).isEqualTo(403);
        assertThat(estado(peloAdministradorDaInstituicao)).isEqualTo(403);
        assertThat(administradoresDe(instituicaoA, outra)).isEqualTo(0);
    }

    @Test
    void designarNaoTocaNosVinculosDeOutraInstituicao() throws Exception {
        UUID naB = novoVinculo(pessoa, instituicaoB, "DOCENTE", "ACTIVO");

        designar(entrar(emailGeral), instituicaoA, emailPessoa);

        assertThat(contar("SELECT count(*) FROM vinculo_papel WHERE vinculo_id = ?", naB)).isEqualTo(0);
        assertThat(administradoresDe(instituicaoB, pessoa)).isEqualTo(0);
        assertThat(contar("SELECT count(*) FROM vinculo_institucional WHERE id = ? AND estado = 'ACTIVO'", naB))
            .isEqualTo(1);
    }
}

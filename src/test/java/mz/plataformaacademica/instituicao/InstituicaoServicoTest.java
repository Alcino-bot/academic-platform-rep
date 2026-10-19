package mz.plataformaacademica.instituicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import mz.plataformaacademica.comum.ConflitoException;
import mz.plataformaacademica.comum.NaoEncontradoException;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InstituicaoServicoTest {

    @Autowired
    private InstituicaoServico servico;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager em;

    private UUID tipoAprovado() {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO tipo_instituicao (id, nome, estado, data_aprovacao) VALUES (?, ?, 'APROVADA', now())",
            id, "Tipo " + id);
        return id;
    }

    private UUID tipoPendente() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tipo_instituicao (id, nome) VALUES (?, ?)", id, "Tipo " + id);
        return id;
    }

    private void gravarELimparCache() {
        em.flush();
        em.clear();
    }

    @Test
    void criaInstituicaoPendenteComTextosSemEspacosAExtremos() {
        UUID tipo = tipoAprovado();

        Instituicao criada = servico.criar("  Universidade Teste  ", " UT ", tipo);
        gravarELimparCache();
        Instituicao lida = servico.obter(criada.getId());

        assertThat(lida.getId()).isNotNull();
        assertThat(lida.getNome()).isEqualTo("Universidade Teste");
        assertThat(lida.getSigla()).isEqualTo("UT");
        assertThat(lida.getTipoInstituicaoId()).isEqualTo(tipo);
        assertThat(lida.getEstado()).isEqualTo(EstadoInstituicao.PENDENTE);
        assertThat(lida.getDataCriacao()).isNotNull();
    }

    @Test
    void recusaNomeDuplicadoIgnorandoMaiusculasEEspacos() {
        UUID tipo = tipoAprovado();
        servico.criar("Universidade Teste", "UT1", tipo);

        assertThatThrownBy(() -> servico.criar("  universidade teste ", "UT2", tipo))
            .isInstanceOf(ConflitoException.class)
            .hasMessageContaining("nome");
    }

    @Test
    void recusaSiglaDuplicadaIgnorandoMaiusculasEEspacos() {
        UUID tipo = tipoAprovado();
        servico.criar("Universidade Um", "UT", tipo);

        assertThatThrownBy(() -> servico.criar("Universidade Dois", " ut ", tipo))
            .isInstanceOf(ConflitoException.class)
            .hasMessageContaining("sigla");
    }

    @Test
    void recusaTipoAindaNaoAprovado() {
        UUID tipo = tipoPendente();

        assertThatThrownBy(() -> servico.criar("Instituto Teste", "IT", tipo))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("aprovado");
    }

    @Test
    void recusaTipoInexistente() {
        assertThatThrownBy(() -> servico.criar("Instituto Teste", "IT", UUID.randomUUID()))
            .isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void recusaNomeSiglaOuTipoEmFalta() {
        UUID tipo = tipoAprovado();

        assertThatThrownBy(() -> servico.criar("   ", "IT", tipo))
            .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> servico.criar("Instituto Teste", null, tipo))
            .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> servico.criar("Instituto Teste", "IT", null))
            .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void mudaEstadoEGravaNaBaseDeDados() {
        UUID tipo = tipoAprovado();
        Instituicao criada = servico.criar("Escola Teste", "ET", tipo);

        servico.mudarEstado(criada.getId(), EstadoInstituicao.ACTIVA);
        gravarELimparCache();

        assertThat(servico.obter(criada.getId()).getEstado()).isEqualTo(EstadoInstituicao.ACTIVA);
    }

    @Test
    void recusaTransicaoDeEstadoNaoPermitida() {
        UUID tipo = tipoAprovado();
        Instituicao criada = servico.criar("Escola Teste", "ET", tipo);

        assertThatThrownBy(() -> servico.mudarEstado(criada.getId(), EstadoInstituicao.SUSPENSA))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("PENDENTE");
        assertThat(servico.obter(criada.getId()).getEstado()).isEqualTo(EstadoInstituicao.PENDENTE);
    }

    @Test
    void instituicaoInactivaNaoVoltaAActiva() {
        UUID tipo = tipoAprovado();
        Instituicao criada = servico.criar("Escola Teste", "ET", tipo);
        servico.mudarEstado(criada.getId(), EstadoInstituicao.ACTIVA);
        servico.mudarEstado(criada.getId(), EstadoInstituicao.INACTIVA);

        assertThatThrownBy(() -> servico.mudarEstado(criada.getId(), EstadoInstituicao.ACTIVA))
            .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void obterInstituicaoInexistenteFalha() {
        assertThatThrownBy(() -> servico.obter(UUID.randomUUID()))
            .isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void listaInstituicoesOrdenadasPorNome() {
        UUID tipo = tipoAprovado();
        servico.criar("Beta Instituto", "BI", tipo);
        servico.criar("Alfa Instituto", "AI", tipo);

        assertThat(servico.listar())
            .extracting(Instituicao::getNome)
            .containsSubsequence("Alfa Instituto", "Beta Instituto");
    }
}

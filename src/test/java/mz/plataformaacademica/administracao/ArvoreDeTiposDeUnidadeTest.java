package mz.plataformaacademica.administracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import mz.plataformaacademica.administracao.ArvoreDeTiposDeUnidade.Entrada;
import mz.plataformaacademica.administracao.ArvoreDeTiposDeUnidade.TipoResolvido;
import mz.plataformaacademica.comum.RegraDeNegocioException;

class ArvoreDeTiposDeUnidadeTest {

    private static Entrada e(String nome, String pai) {
        return new Entrada(nome, pai);
    }

    private static List<String> nomes(List<TipoResolvido> resolvidos) {
        return resolvidos.stream().map(TipoResolvido::nome).toList();
    }

    private static List<Integer> niveis(List<TipoResolvido> resolvidos) {
        return resolvidos.stream().map(TipoResolvido::nivel).toList();
    }

    @Test
    void onivelEDerivadoDoPai() {
        List<TipoResolvido> resolvidos = ArvoreDeTiposDeUnidade.resolver(List.of(
            e("Faculdade", null), e("Departamento", "Faculdade"), e("Curso", "Departamento")));

        assertThat(nomes(resolvidos)).containsExactly("Faculdade", "Departamento", "Curso");
        assertThat(niveis(resolvidos)).containsExactly(1, 2, 3);
    }

    @Test
    void osPaisSaemSempreAntesDosFilhosMesmoQuandoVemDepois() {
        List<TipoResolvido> resolvidos = ArvoreDeTiposDeUnidade.resolver(List.of(
            e("Curso", "Departamento"), e("Departamento", "Faculdade"), e("Faculdade", null)));

        assertThat(nomes(resolvidos)).containsExactly("Faculdade", "Departamento", "Curso");
        assertThat(niveis(resolvidos)).containsExactly(1, 2, 3);
    }

    @Test
    void variasRaizesSaoPermitidas() {
        List<TipoResolvido> resolvidos = ArvoreDeTiposDeUnidade.resolver(List.of(
            e("Faculdade", null), e("Direccao", null), e("Departamento", "Faculdade")));

        assertThat(nomes(resolvidos)).containsExactly("Faculdade", "Direccao", "Departamento");
        assertThat(niveis(resolvidos)).containsExactly(1, 1, 2);
    }

    @Test
    void oPaiEIdentificadoSemDistinguirMaiusculasNemEspacos() {
        List<TipoResolvido> resolvidos = ArvoreDeTiposDeUnidade.resolver(List.of(
            e("Faculdade", null), e("Departamento", "  FACULDADE ")));

        assertThat(niveis(resolvidos)).containsExactly(1, 2);
    }

    @Test
    void osNomesSaoLimpos() {
        List<TipoResolvido> resolvidos = ArvoreDeTiposDeUnidade.resolver(List.of(e("  Faculdade  ", "   ")));

        assertThat(nomes(resolvidos)).containsExactly("Faculdade");
        assertThat(niveis(resolvidos)).containsExactly(1);
    }

    @Test
    void listaVaziaERecusada() {
        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(List.of()))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("pelo menos um tipo de unidade");
    }

    @Test
    void listaNulaERecusada() {
        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(null))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("pelo menos um tipo de unidade");
    }

    @Test
    void maisDeCinquentaTiposSaoRecusados() {
        List<Entrada> entradas = new ArrayList<>();
        for (int i = 0; i <= ArvoreDeTiposDeUnidade.MAXIMO_DE_TIPOS; i++) {
            entradas.add(e("Tipo " + i, null));
        }

        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(entradas))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("mais de 50");
    }

    @Test
    void cinquentaTiposSaoAceites() {
        List<Entrada> entradas = new ArrayList<>();
        for (int i = 0; i < ArvoreDeTiposDeUnidade.MAXIMO_DE_TIPOS; i++) {
            entradas.add(e("Tipo " + i, null));
        }

        assertThat(ArvoreDeTiposDeUnidade.resolver(entradas).size()).isEqualTo(50);
    }

    @Test
    void nomeEmBrancoERecusado() {
        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(List.of(e("Faculdade", null), e("   ", null))))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("nome de cada tipo de unidade é obrigatório");
    }

    @Test
    void nomeNuloERecusado() {
        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(List.of(e(null, null))))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("nome de cada tipo de unidade é obrigatório");
    }

    @Test
    void nomeComMaisDeCemCaracteresERecusado() {
        String longo = "x".repeat(101);

        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(List.of(e(longo, null))))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("demasiado longo");
    }

    @Test
    void nomeRepetidoERecusadoSemDistinguirMaiusculasNemEspacos() {
        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(List.of(e("Curso", null), e(" curso ", null))))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("mais de uma vez");
    }

    @Test
    void paiInexistenteERecusado() {
        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(List.of(e("Faculdade", null), e("Curso", "Escola"))))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("não existe no modelo");
    }

    @Test
    void umTipoNaoPodeSerPaiDeSiProprio() {
        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(List.of(e("Faculdade", null), e("Curso", "curso"))))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("pai de si próprio");
    }

    @Test
    void cicloEntreDoisTiposERecusado() {
        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(List.of(e("A", "B"), e("B", "A"))))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("ciclo");
    }

    @Test
    void cicloAfastadoDaRaizERecusado() {
        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(List.of(e("Raiz", null), e("A", "B"), e("B", "A"))))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("ciclo");
    }

    @Test
    void cicloDeTresTiposERecusado() {
        assertThatThrownBy(() -> ArvoreDeTiposDeUnidade.resolver(List.of(e("A", "C"), e("B", "A"), e("C", "B"))))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("ciclo");
    }
}

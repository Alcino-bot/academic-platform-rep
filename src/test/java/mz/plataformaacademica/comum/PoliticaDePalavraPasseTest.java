package mz.plataformaacademica.comum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PoliticaDePalavraPasseTest {

    @Test
    void aceitaUmaPalavraPasseValida() {
        PoliticaDePalavraPasse.validar("UmaSenhaBoa123");
    }

    @Test
    void recusaPalavraPasseCurta() {
        assertThatThrownBy(() -> PoliticaDePalavraPasse.validar("curta"))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("10");
    }

    @Test
    void recusaPalavraPasseNula() {
        assertThatThrownBy(() -> PoliticaDePalavraPasse.validar(null))
            .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void recusaPalavraPasseSoComEspacos() {
        assertThatThrownBy(() -> PoliticaDePalavraPasse.validar(" ".repeat(12)))
            .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void aceitaExactamente72Bytes() {
        PoliticaDePalavraPasse.validar("a".repeat(72));
    }

    @Test
    void recusaMaisDe72Bytes() {
        assertThatThrownBy(() -> PoliticaDePalavraPasse.validar("a".repeat(73)))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("72");
    }

    @Test
    void contaBytesEnaoCaracteres() {
        PoliticaDePalavraPasse.validar("\u00e7".repeat(36));
        assertThatThrownBy(() -> PoliticaDePalavraPasse.validar("\u00e7".repeat(37)))
            .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void cabeNoBcryptRespeitaOLimite() {
        assertThat(PoliticaDePalavraPasse.cabeNoBcrypt("a".repeat(72))).isTrue();
        assertThat(PoliticaDePalavraPasse.cabeNoBcrypt("a".repeat(73))).isFalse();
        assertThat(PoliticaDePalavraPasse.cabeNoBcrypt(null)).isFalse();
    }
}

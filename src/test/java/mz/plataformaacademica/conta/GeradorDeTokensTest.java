package mz.plataformaacademica.conta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeradorDeTokensTest {

    @Test
    void tokenTem43CaracteresSeguros() {
        String token = GeradorDeTokens.novo();

        assertThat(token.length()).isEqualTo(43);
        assertThat(token.matches("[A-Za-z0-9_-]+")).isTrue();
    }

    @Test
    void doisTokensSaoDiferentes() {
        assertThat(GeradorDeTokens.novo()).isNotEqualTo(GeradorDeTokens.novo());
    }

    @Test
    void hashEstaCertoParaUmValorConhecido() {
        assertThat(GeradorDeTokens.hash("abc"))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void hashTem64CaracteresHexadecimaisMinusculos() {
        assertThat(GeradorDeTokens.hash(GeradorDeTokens.novo()).matches("[0-9a-f]{64}")).isTrue();
    }

    @Test
    void hashNaoContemOToken() {
        String token = GeradorDeTokens.novo();

        assertThat(GeradorDeTokens.hash(token).contains(token)).isFalse();
    }
}

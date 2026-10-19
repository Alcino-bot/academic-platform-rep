package mz.plataformaacademica.instituicao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoInstituicaoTest {

    @Test
    void pendenteSoPodeIrParaActiva() {
        assertThat(EstadoInstituicao.PENDENTE.podeTransitarPara(EstadoInstituicao.ACTIVA)).isTrue();
        assertThat(EstadoInstituicao.PENDENTE.podeTransitarPara(EstadoInstituicao.SUSPENSA)).isFalse();
        assertThat(EstadoInstituicao.PENDENTE.podeTransitarPara(EstadoInstituicao.INACTIVA)).isFalse();
        assertThat(EstadoInstituicao.PENDENTE.podeTransitarPara(EstadoInstituicao.PENDENTE)).isFalse();
    }

    @Test
    void activaPodeIrParaSuspensaOuInactiva() {
        assertThat(EstadoInstituicao.ACTIVA.podeTransitarPara(EstadoInstituicao.SUSPENSA)).isTrue();
        assertThat(EstadoInstituicao.ACTIVA.podeTransitarPara(EstadoInstituicao.INACTIVA)).isTrue();
        assertThat(EstadoInstituicao.ACTIVA.podeTransitarPara(EstadoInstituicao.PENDENTE)).isFalse();
        assertThat(EstadoInstituicao.ACTIVA.podeTransitarPara(EstadoInstituicao.ACTIVA)).isFalse();
    }

    @Test
    void suspensaPodeVoltarAActivaOuIrParaInactiva() {
        assertThat(EstadoInstituicao.SUSPENSA.podeTransitarPara(EstadoInstituicao.ACTIVA)).isTrue();
        assertThat(EstadoInstituicao.SUSPENSA.podeTransitarPara(EstadoInstituicao.INACTIVA)).isTrue();
        assertThat(EstadoInstituicao.SUSPENSA.podeTransitarPara(EstadoInstituicao.PENDENTE)).isFalse();
    }

    @Test
    void inactivaNaoPodeSairDoEstado() {
        for (EstadoInstituicao destino : EstadoInstituicao.values()) {
            assertThat(EstadoInstituicao.INACTIVA.podeTransitarPara(destino)).isFalse();
        }
    }
}

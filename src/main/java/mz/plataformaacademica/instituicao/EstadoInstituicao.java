package mz.plataformaacademica.instituicao;

public enum EstadoInstituicao {
    PENDENTE,
    ACTIVA,
    SUSPENSA,
    INACTIVA;

    public boolean podeTransitarPara(EstadoInstituicao destino) {
        return switch (this) {
            case PENDENTE -> destino == ACTIVA;
            case ACTIVA -> destino == SUSPENSA || destino == INACTIVA;
            case SUSPENSA -> destino == ACTIVA || destino == INACTIVA;
            case INACTIVA -> false;
        };
    }
}

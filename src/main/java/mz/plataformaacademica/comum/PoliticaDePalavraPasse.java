package mz.plataformaacademica.comum;

import java.nio.charset.StandardCharsets;

public final class PoliticaDePalavraPasse {

    public static final int MINIMO_CARACTERES = 10;
    public static final int MAXIMO_BYTES = 72;

    private PoliticaDePalavraPasse() {
    }

    public static boolean cabeNoBcrypt(String palavraPasse) {
        return palavraPasse != null
            && palavraPasse.getBytes(StandardCharsets.UTF_8).length <= MAXIMO_BYTES;
    }

    public static void validar(String palavraPasse) {
        if (palavraPasse == null
                || palavraPasse.isBlank()
                || palavraPasse.length() < MINIMO_CARACTERES) {
            throw new RegraDeNegocioException(
                "A palavra-passe deve ter pelo menos " + MINIMO_CARACTERES + " caracteres");
        }
        if (!cabeNoBcrypt(palavraPasse)) {
            throw new RegraDeNegocioException(
                "A palavra-passe é demasiado longa (máximo de " + MAXIMO_BYTES + " bytes)");
        }
    }
}

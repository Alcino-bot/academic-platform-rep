package mz.plataformaacademica.conta;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class GeradorDeTokens {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private GeradorDeTokens() {
    }

    public static String novo() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            MessageDigest resumo = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(resumo.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não está disponível", e);
        }
    }
}

package mz.plataformaacademica.autenticacao;

import java.util.UUID;

public record RegistoUtilizador(
    UUID id,
    String email,
    String nomeCompleto,
    String palavraPasseHash,
    String estado,
    boolean bloqueada) {
}

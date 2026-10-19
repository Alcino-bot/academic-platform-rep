package mz.plataformaacademica.autenticacao;

import java.util.UUID;

public record PrincipalPad(UUID utilizadorId, String email, String nome, UUID instituicaoActivaId) {
}

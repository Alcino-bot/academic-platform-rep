package mz.plataformaacademica.autenticacao;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import mz.plataformaacademica.comum.CredenciaisInvalidasException;

@Component
public class UtilizadorActual {

    public UUID id() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof PrincipalPad principal) {
            return principal.utilizadorId();
        }
        throw new CredenciaisInvalidasException("Sessão inválida");
    }
}

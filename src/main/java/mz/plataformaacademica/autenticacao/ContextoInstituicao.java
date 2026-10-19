package mz.plataformaacademica.autenticacao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import mz.plataformaacademica.comum.AcessoNegadoException;

@Component
public class ContextoInstituicao {

    public Optional<UUID> instituicaoActiva() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof PrincipalPad principal) {
            return Optional.ofNullable(principal.instituicaoActivaId());
        }
        return Optional.empty();
    }

    public UUID exigir() {
        return instituicaoActiva()
            .orElseThrow(() -> new AcessoNegadoException("Escolha primeiro uma instituição"));
    }
}

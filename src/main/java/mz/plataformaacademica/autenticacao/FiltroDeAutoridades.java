package mz.plataformaacademica.autenticacao;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class FiltroDeAutoridades extends OncePerRequestFilter {

    private final ServicoDeAutenticacao servico;

    public FiltroDeAutoridades(ServicoDeAutenticacao servico) {
        this.servico = servico;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();
        if (actual != null && actual.getPrincipal() instanceof PrincipalPad base) {
            if (!servico.contaActiva(base.utilizadorId())) {
                HttpSession sessao = pedido.getSession(false);
                if (sessao != null) {
                    sessao.invalidate();
                }
                SecurityContextHolder.clearContext();
            } else {
                refrescar(pedido, base);
            }
        }
        cadeia.doFilter(pedido, resposta);
    }

    private void refrescar(HttpServletRequest pedido, PrincipalPad base) {
        UUID instituicao = null;
        HttpSession sessao = pedido.getSession(false);
        if (sessao != null && sessao.getAttribute(SessaoPad.INSTITUICAO_ACTIVA) instanceof UUID guardada) {
            if (servico.podeActuarNaInstituicao(base.utilizadorId(), guardada)) {
                instituicao = guardada;
            } else {
                sessao.removeAttribute(SessaoPad.INSTITUICAO_ACTIVA);
            }
        }
        PrincipalPad principal = new PrincipalPad(
            base.utilizadorId(), base.email(), base.nome(), instituicao);
        Authentication autenticacao = new UsernamePasswordAuthenticationToken(
            principal, null, servico.autoridades(base.utilizadorId(), instituicao));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
    }
}

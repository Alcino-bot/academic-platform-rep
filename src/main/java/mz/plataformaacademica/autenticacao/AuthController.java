package mz.plataformaacademica.autenticacao;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mz.plataformaacademica.comum.CredenciaisInvalidasException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record PedidoEntrar(
        @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(max = 200) String palavraPasse) {
    }

    public record PedidoInstituicao(@NotNull UUID instituicaoId) {
    }

    public record RespostaSessao(
        UUID utilizadorId,
        String email,
        String nome,
        UUID instituicaoActivaId,
        List<InstituicaoDisponivel> instituicoesDisponiveis,
        List<String> autoridades,
        String csrfCabecalho,
        String csrfToken) {
    }

    private final ServicoDeAutenticacao servico;
    private final CsrfTokenRepository repositorioCsrf;
    private final SecurityContextRepository repositorioDeContexto = new HttpSessionSecurityContextRepository();

    public AuthController(ServicoDeAutenticacao servico, CsrfTokenRepository repositorioCsrf) {
        this.servico = servico;
        this.repositorioCsrf = repositorioCsrf;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(HttpServletRequest pedido, HttpServletResponse resposta) {
        CsrfToken token = repositorioCsrf.loadToken(pedido);
        if (token == null) {
            token = repositorioCsrf.generateToken(pedido);
            repositorioCsrf.saveToken(token, pedido, resposta);
        }
        return Map.of("cabecalho", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/entrar")
    public RespostaSessao entrar(
            @Valid @RequestBody PedidoEntrar corpo, HttpServletRequest pedido, HttpServletResponse resposta) {
        PrincipalPad principal = servico.entrar(corpo.email(), corpo.palavraPasse());
        if (pedido.getSession(false) != null) {
            pedido.changeSessionId();
        }
        List<GrantedAuthority> autoridades = servico.autoridades(principal.utilizadorId(), null);
        Authentication autenticacao = new UsernamePasswordAuthenticationToken(principal, null, autoridades);
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        repositorioDeContexto.saveContext(contexto, pedido, resposta);
        pedido.getSession().removeAttribute(SessaoPad.INSTITUICAO_ACTIVA);
        CsrfToken novoToken = repositorioCsrf.generateToken(pedido);
        repositorioCsrf.saveToken(novoToken, pedido, resposta);
        return resposta(principal, autoridades, novoToken.getHeaderName(), novoToken.getToken());
    }

    @GetMapping("/eu")
    public RespostaSessao eu() {
        PrincipalPad principal = principalActual();
        List<GrantedAuthority> autoridades = servico.autoridades(
            principal.utilizadorId(), principal.instituicaoActivaId());
        return resposta(principal, autoridades, null, null);
    }

    @PostMapping("/instituicao")
    public RespostaSessao escolherInstituicao(
            @Valid @RequestBody PedidoInstituicao corpo, HttpServletRequest pedido) {
        PrincipalPad actual = principalActual();
        servico.validarEscolhaDeInstituicao(actual.utilizadorId(), corpo.instituicaoId());
        pedido.getSession().setAttribute(SessaoPad.INSTITUICAO_ACTIVA, corpo.instituicaoId());
        PrincipalPad novo = new PrincipalPad(
            actual.utilizadorId(), actual.email(), actual.nome(), corpo.instituicaoId());
        List<GrantedAuthority> autoridades = servico.autoridades(novo.utilizadorId(), corpo.instituicaoId());
        return resposta(novo, autoridades, null, null);
    }

    @PostMapping("/sair")
    public ResponseEntity<Void> sair(HttpServletRequest pedido) {
        HttpSession sessao = pedido.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private PrincipalPad principalActual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof PrincipalPad principal) {
            return principal;
        }
        throw new CredenciaisInvalidasException("Sessão inválida");
    }

    private RespostaSessao resposta(
            PrincipalPad principal, List<GrantedAuthority> autoridades, String csrfCabecalho, String csrfToken) {
        List<String> nomes = autoridades.stream().map(GrantedAuthority::getAuthority).sorted().toList();
        return new RespostaSessao(
            principal.utilizadorId(),
            principal.email(),
            principal.nome(),
            principal.instituicaoActivaId(),
            servico.instituicoesDisponiveis(principal.utilizadorId()),
            nomes,
            csrfCabecalho,
            csrfToken);
    }
}

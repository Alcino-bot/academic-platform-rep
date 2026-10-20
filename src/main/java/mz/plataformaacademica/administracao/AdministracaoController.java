package mz.plataformaacademica.administracao;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mz.plataformaacademica.administracao.ServicoDeTiposDeInstituicao.TipoVista;
import mz.plataformaacademica.autenticacao.PrincipalPad;
import mz.plataformaacademica.comum.CredenciaisInvalidasException;
import mz.plataformaacademica.instituicao.EstadoInstituicao;
import mz.plataformaacademica.instituicao.Instituicao;
import mz.plataformaacademica.instituicao.InstituicaoServico;

@RestController
@RequestMapping("/api/administracao")
public class AdministracaoController {

    public record PedidoTipo(
        @NotBlank @Size(max = 100) String nome,
        @Size(max = 2000) String descricao) {
    }

    public record PedidoInstituicao(
        @NotBlank @Size(max = 200) String nome,
        @NotBlank @Size(max = 30) String sigla,
        @NotNull UUID modeloId) {
    }

    public record PedidoEstado(@NotNull EstadoInstituicao estado) {
    }

    public record InstituicaoVista(
        UUID id, String nome, String sigla, UUID tipoInstituicaoId, UUID modeloOrigemId, String estado) {
    }

    private final ServicoDeTiposDeInstituicao tipos;
    private final ServicoDeInstituicoesAdministradas administradas;
    private final InstituicaoServico instituicoes;

    public AdministracaoController(
            ServicoDeTiposDeInstituicao tipos,
            ServicoDeInstituicoesAdministradas administradas,
            InstituicaoServico instituicoes) {
        this.tipos = tipos;
        this.administradas = administradas;
        this.instituicoes = instituicoes;
    }

    @GetMapping("/tipos-instituicao")
    public List<TipoVista> listarTipos() {
        return tipos.listar();
    }

    @PostMapping("/tipos-instituicao")
    public ResponseEntity<TipoVista> proporTipo(@Valid @RequestBody PedidoTipo corpo) {
        TipoVista criado = tipos.propor(corpo.nome(), corpo.descricao(), actor());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PostMapping("/tipos-instituicao/{id}/aprovar")
    public TipoVista aprovarTipo(@PathVariable UUID id) {
        return tipos.aprovar(id, actor());
    }

    @PostMapping("/tipos-instituicao/{id}/rejeitar")
    public TipoVista rejeitarTipo(@PathVariable UUID id) {
        return tipos.rejeitar(id, actor());
    }

    @GetMapping("/instituicoes")
    public List<InstituicaoVista> listarInstituicoes() {
        return instituicoes.listar().stream().map(AdministracaoController::vista).toList();
    }

    @PostMapping("/instituicoes")
    public ResponseEntity<InstituicaoVista> criarInstituicao(@Valid @RequestBody PedidoInstituicao corpo) {
        Instituicao criada = administradas.criarActivaAPartirDoModelo(corpo.nome(), corpo.sigla(), corpo.modeloId());
        return ResponseEntity.status(HttpStatus.CREATED).body(vista(criada));
    }

    @PostMapping("/instituicoes/{id}/estado")
    public InstituicaoVista mudarEstado(@PathVariable UUID id, @Valid @RequestBody PedidoEstado corpo) {
        return vista(instituicoes.mudarEstado(id, corpo.estado()));
    }

    private static InstituicaoVista vista(Instituicao instituicao) {
        return new InstituicaoVista(
            instituicao.getId(),
            instituicao.getNome(),
            instituicao.getSigla(),
            instituicao.getTipoInstituicaoId(),
            instituicao.getModeloOrigemId(),
            instituicao.getEstado().name());
    }

    private static UUID actor() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof PrincipalPad principal) {
            return principal.utilizadorId();
        }
        throw new CredenciaisInvalidasException("Sessão inválida");
    }
}

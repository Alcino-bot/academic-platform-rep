package mz.plataformaacademica.vinculo;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mz.plataformaacademica.autenticacao.ContextoInstituicao;
import mz.plataformaacademica.autenticacao.UtilizadorActual;
import mz.plataformaacademica.vinculo.ServicoDeVinculos.VinculoVista;

@RestController
@RequestMapping("/api/instituicao/vinculos")
public class AdministracaoDeVinculosController {

    public record PedidoDeRejeicao(@NotBlank @Size(max = 500) String motivo) {
    }

    public record PedidoDePapel(@NotBlank String papel) {
    }

    private final ServicoDeVinculos vinculos;
    private final ContextoInstituicao contexto;
    private final UtilizadorActual utilizador;

    public AdministracaoDeVinculosController(
            ServicoDeVinculos vinculos, ContextoInstituicao contexto, UtilizadorActual utilizador) {
        this.vinculos = vinculos;
        this.contexto = contexto;
        this.utilizador = utilizador;
    }

    @GetMapping
    public List<VinculoVista> listar(@RequestParam(required = false) String estado) {
        return vinculos.daInstituicao(contexto.exigir(), estado);
    }

    @PostMapping("/{id}/aprovar")
    public VinculoVista aprovar(@PathVariable UUID id) {
        return vinculos.aprovar(contexto.exigir(), id, utilizador.id());
    }

    @PostMapping("/{id}/rejeitar")
    public VinculoVista rejeitar(@PathVariable UUID id, @Valid @RequestBody PedidoDeRejeicao corpo) {
        return vinculos.rejeitar(contexto.exigir(), id, utilizador.id(), corpo.motivo());
    }

    @PostMapping("/{id}/papeis")
    public VinculoVista atribuirPapel(@PathVariable UUID id, @Valid @RequestBody PedidoDePapel corpo) {
        return vinculos.atribuirPapel(contexto.exigir(), id, corpo.papel(), utilizador.id());
    }

    @PostMapping("/{id}/papeis/{papel}/revogar")
    public VinculoVista revogarPapel(@PathVariable UUID id, @PathVariable String papel) {
        return vinculos.revogarPapel(contexto.exigir(), id, papel);
    }
}

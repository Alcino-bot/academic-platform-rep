package mz.plataformaacademica.administracao;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mz.plataformaacademica.administracao.ArvoreDeTiposDeUnidade.Entrada;
import mz.plataformaacademica.administracao.ServicoDeModelosOrganizacionais.ModeloResumo;
import mz.plataformaacademica.administracao.ServicoDeModelosOrganizacionais.ModeloVista;

@RestController
@RequestMapping("/api/administracao/modelos-organizacionais")
public class ModelosOrganizacionaisController {

    public record PedidoTipoDeUnidade(
        @NotBlank @Size(max = 100) String nome,
        @Size(max = 100) String paiNome) {
    }

    public record PedidoModelo(
        @NotNull UUID tipoInstituicaoId,
        @NotBlank @Size(max = 100) String nome,
        @Size(max = 2000) String descricao,
        @NotEmpty @Size(max = ArvoreDeTiposDeUnidade.MAXIMO_DE_TIPOS) List<@NotNull @Valid PedidoTipoDeUnidade> tiposDeUnidade) {
    }

    private final ServicoDeModelosOrganizacionais modelos;

    public ModelosOrganizacionaisController(ServicoDeModelosOrganizacionais modelos) {
        this.modelos = modelos;
    }

    @GetMapping
    public List<ModeloResumo> listar(@RequestParam(required = false) String estado) {
        return modelos.listar(estado);
    }

    @GetMapping("/{id}")
    public ModeloVista obter(@PathVariable UUID id) {
        return modelos.obter(id);
    }

    @PostMapping
    public ResponseEntity<ModeloVista> criar(@Valid @RequestBody PedidoModelo corpo) {
        List<Entrada> entradas = corpo.tiposDeUnidade().stream()
            .map(tipo -> new Entrada(tipo.nome(), tipo.paiNome()))
            .toList();
        ModeloVista criado = modelos.criar(corpo.tipoInstituicaoId(), corpo.nome(), corpo.descricao(), entradas);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PostMapping("/{id}/activar")
    public ModeloVista activar(@PathVariable UUID id) {
        return modelos.activar(id);
    }

    @PostMapping("/{id}/inactivar")
    public ModeloVista inactivar(@PathVariable UUID id) {
        return modelos.inactivar(id);
    }
}

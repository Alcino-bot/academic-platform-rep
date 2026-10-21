package mz.plataformaacademica.vinculo;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import mz.plataformaacademica.autenticacao.UtilizadorActual;
import mz.plataformaacademica.vinculo.ServicoDeVinculos.VinculoVista;

@RestController
@RequestMapping("/api/vinculos")
public class VinculoController {

    public record PedidoDeVinculo(@NotNull UUID instituicaoId, @NotBlank String tipoVinculo) {
    }

    private final ServicoDeVinculos vinculos;
    private final UtilizadorActual utilizador;

    public VinculoController(ServicoDeVinculos vinculos, UtilizadorActual utilizador) {
        this.vinculos = vinculos;
        this.utilizador = utilizador;
    }

    @PostMapping("/pedidos")
    public ResponseEntity<VinculoVista> pedir(@Valid @RequestBody PedidoDeVinculo corpo) {
        VinculoVista criado = vinculos.pedir(utilizador.id(), corpo.instituicaoId(), corpo.tipoVinculo());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping("/meus")
    public List<VinculoVista> meus() {
        return vinculos.meus(utilizador.id());
    }
}

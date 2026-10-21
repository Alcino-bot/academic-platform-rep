package mz.plataformaacademica.administracao;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mz.plataformaacademica.administracao.ServicoDeAdministradoresDeInstituicao.Designacao;
import mz.plataformaacademica.autenticacao.UtilizadorActual;

@RestController
@RequestMapping("/api/administracao/instituicoes/{instituicaoId}/administradores")
public class AdministradoresController {

    public record PedidoDeDesignacao(@NotBlank @Size(max = 255) String email) {
    }

    private final ServicoDeAdministradoresDeInstituicao administradores;
    private final UtilizadorActual utilizador;

    public AdministradoresController(
            ServicoDeAdministradoresDeInstituicao administradores, UtilizadorActual utilizador) {
        this.administradores = administradores;
        this.utilizador = utilizador;
    }

    @PostMapping
    public Designacao designar(@PathVariable UUID instituicaoId, @Valid @RequestBody PedidoDeDesignacao corpo) {
        return administradores.designar(instituicaoId, corpo.email(), utilizador.id());
    }
}

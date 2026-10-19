package mz.plataformaacademica.conta;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/contas")
public class ContaController {

    static final String MENSAGEM_REGISTO =
        "Se o email puder ser registado, enviámos uma ligação para activar a conta.";

    public record PedidoRegisto(
        @NotBlank @Size(max = 200) String nomeCompleto,
        @NotBlank @Size(max = 255) String email) {
    }

    public record PedidoEmail(@NotBlank @Size(max = 255) String email) {
    }

    public record PedidoActivacao(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Size(max = 200) String palavraPasse) {
    }

    public record Mensagem(String mensagem) {
    }

    private final ServicoDeContas servico;

    public ContaController(ServicoDeContas servico) {
        this.servico = servico;
    }

    @PostMapping("/registar")
    public ResponseEntity<Mensagem> registar(@Valid @RequestBody PedidoRegisto corpo) {
        servico.registar(corpo.nomeCompleto(), corpo.email());
        return ResponseEntity.accepted().body(new Mensagem(MENSAGEM_REGISTO));
    }

    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<Mensagem> reenviar(@Valid @RequestBody PedidoEmail corpo) {
        servico.reenviarVerificacao(corpo.email());
        return ResponseEntity.accepted().body(new Mensagem(MENSAGEM_REGISTO));
    }

    @PostMapping("/activar")
    public ResponseEntity<Mensagem> activar(@Valid @RequestBody PedidoActivacao corpo) {
        servico.activar(corpo.token(), corpo.palavraPasse());
        return ResponseEntity.ok(new Mensagem("Conta activada. Já pode entrar."));
    }
}

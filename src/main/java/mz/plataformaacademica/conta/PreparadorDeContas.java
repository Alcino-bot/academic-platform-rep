package mz.plataformaacademica.conta;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreparadorDeContas {

    static final String TIPO_VERIFICACAO = "VERIFICACAO_EMAIL";

    private final RepositorioDeContas repositorio;
    private final long validadeSegundos;
    private final long intervaloMinimoSegundos;

    public PreparadorDeContas(
            RepositorioDeContas repositorio,
            @Value("${conta.verificacao.validade-segundos:86400}") long validadeSegundos,
            @Value("${conta.verificacao.intervalo-minimo-segundos:120}") long intervaloMinimoSegundos) {
        this.repositorio = repositorio;
        this.validadeSegundos = validadeSegundos;
        this.intervaloMinimoSegundos = intervaloMinimoSegundos;
    }

    @Transactional
    public Optional<EmailDeVerificacao> prepararParaRegisto(String nome, String email, String hashInutilizavel) {
        Optional<ContaExistente> existente = repositorio.procurarPorEmail(email);
        if (existente.isEmpty()) {
            Optional<UUID> criada = repositorio.criarPendente(email, nome, hashInutilizavel);
            if (criada.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(emitir(criada.get(), email, nome));
        }
        return prepararParaConta(existente.get(), email);
    }

    @Transactional
    public Optional<EmailDeVerificacao> prepararParaReenvio(String email) {
        return repositorio.procurarPorEmail(email).flatMap(conta -> prepararParaConta(conta, email));
    }

    private Optional<EmailDeVerificacao> prepararParaConta(ContaExistente conta, String email) {
        if (!"PENDENTE".equals(conta.estado())) {
            return Optional.empty();
        }
        if (repositorio.emitidoRecentemente(conta.id(), TIPO_VERIFICACAO, intervaloMinimoSegundos)) {
            return Optional.empty();
        }
        return Optional.of(emitir(conta.id(), email, conta.nomeCompleto()));
    }

    private EmailDeVerificacao emitir(UUID utilizadorId, String email, String nome) {
        repositorio.invalidarActivos(utilizadorId, TIPO_VERIFICACAO);
        String token = GeradorDeTokens.novo();
        repositorio.guardarToken(utilizadorId, TIPO_VERIFICACAO, GeradorDeTokens.hash(token), validadeSegundos);
        return new EmailDeVerificacao(email, nome, token);
    }
}

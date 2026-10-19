package mz.plataformaacademica.conta;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.plataformaacademica.comum.PoliticaDePalavraPasse;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@Service
public class ServicoDeContas {

    static final String MENSAGEM_LIGACAO_INVALIDA = "Ligação inválida ou expirada";

    private static final Logger REGISTO = LoggerFactory.getLogger(ServicoDeContas.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final PreparadorDeContas preparador;
    private final RepositorioDeContas repositorio;
    private final PasswordEncoder codificador;
    private final EnviadorDeEmail enviador;
    private final String urlBase;

    public ServicoDeContas(
            PreparadorDeContas preparador,
            RepositorioDeContas repositorio,
            PasswordEncoder codificador,
            EnviadorDeEmail enviador,
            @Value("${aplicacao.url-base:http://localhost:3000}") String urlBase) {
        this.preparador = preparador;
        this.repositorio = repositorio;
        this.codificador = codificador;
        this.enviador = enviador;
        this.urlBase = urlBase;
    }

    public void registar(String nomeCompleto, String email) {
        String nome = nomeValido(nomeCompleto);
        String endereco = emailValido(email);
        String hashInutilizavel = codificador.encode(GeradorDeTokens.novo());
        preparador.prepararParaRegisto(nome, endereco, hashInutilizavel).ifPresent(this::enviarVerificacao);
    }

    public void reenviarVerificacao(String email) {
        String endereco = emailValido(email);
        preparador.prepararParaReenvio(endereco).ifPresent(this::enviarVerificacao);
    }

    @Transactional
    public void activar(String token, String palavraPasse) {
        PoliticaDePalavraPasse.validar(palavraPasse);
        Optional<UUID> utilizadorId = repositorio.consumirToken(
            GeradorDeTokens.hash(token), PreparadorDeContas.TIPO_VERIFICACAO);
        if (utilizadorId.isEmpty()
                || !repositorio.activarConta(codificador.encode(palavraPasse), utilizadorId.get())) {
            throw new RegraDeNegocioException(MENSAGEM_LIGACAO_INVALIDA);
        }
    }

    private void enviarVerificacao(EmailDeVerificacao verificacao) {
        String ligacao = urlBase + "/activar-conta?token=" + verificacao.token();
        String texto = "Olá " + verificacao.nome() + ",\n\n"
            + "Para activar a sua conta e definir a sua palavra-passe, abra a ligação abaixo. "
            + "A ligação é válida durante um período limitado e só pode ser usada uma vez.\n"
            + ligacao + "\n\n"
            + "Se não pediu este registo, ignore esta mensagem.\n";
        try {
            enviador.enviar(
                verificacao.destinatario(), "Active a sua conta na Plataforma Académica Digital", texto);
        } catch (RuntimeException e) {
            REGISTO.warn("Falha ao enviar o email de verificação: {}", e.getClass().getSimpleName());
        }
    }

    private static String nomeValido(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            throw new RegraDeNegocioException("O nome é obrigatório");
        }
        String nome = nomeCompleto.strip();
        if (nome.length() > 200) {
            throw new RegraDeNegocioException("O nome é demasiado longo");
        }
        return nome;
    }

    private static String emailValido(String email) {
        String endereco = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        if (endereco.length() > 255 || !EMAIL.matcher(endereco).matches()) {
            throw new RegraDeNegocioException("O email não é válido");
        }
        return endereco;
    }
}

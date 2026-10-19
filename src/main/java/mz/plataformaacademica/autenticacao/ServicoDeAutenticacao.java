package mz.plataformaacademica.autenticacao;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import mz.plataformaacademica.comum.AcessoNegadoException;
import mz.plataformaacademica.comum.CredenciaisInvalidasException;
import mz.plataformaacademica.comum.PoliticaDePalavraPasse;

@Service
public class ServicoDeAutenticacao {

    static final String MENSAGEM_CREDENCIAIS =
        "Email ou palavra-passe inválidos, ou conta temporariamente bloqueada";

    private final RepositorioDeAutenticacao repositorio;
    private final PasswordEncoder codificador;
    private final int maximoTentativas;
    private final long duracaoBloqueioSegundos;
    private final String hashFicticio;

    public ServicoDeAutenticacao(
            RepositorioDeAutenticacao repositorio,
            PasswordEncoder codificador,
            @Value("${seguranca.login.maximo-tentativas:5}") int maximoTentativas,
            @Value("${seguranca.login.duracao-bloqueio-segundos:900}") long duracaoBloqueioSegundos) {
        this.repositorio = repositorio;
        this.codificador = codificador;
        this.maximoTentativas = maximoTentativas;
        this.duracaoBloqueioSegundos = duracaoBloqueioSegundos;
        this.hashFicticio = codificador.encode("palavra-passe-ficticia");
    }

    public PrincipalPad entrar(String email, String palavraPasse) {
        if (!PoliticaDePalavraPasse.cabeNoBcrypt(palavraPasse)) {
            codificador.matches("palavra-passe-ficticia", hashFicticio);
            throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS);
        }
        Optional<RegistoUtilizador> encontrado = repositorio.procurarPorEmail(email);
        if (encontrado.isEmpty()) {
            codificador.matches(palavraPasse, hashFicticio);
            throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS);
        }
        RegistoUtilizador utilizador = encontrado.get();
        if (utilizador.bloqueada()) {
            codificador.matches(palavraPasse, hashFicticio);
            throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS);
        }
        if (!codificador.matches(palavraPasse, utilizador.palavraPasseHash())) {
            repositorio.registarFalha(utilizador.id(), maximoTentativas, duracaoBloqueioSegundos);
            throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS);
        }
        if (!"ACTIVO".equals(utilizador.estado())) {
            throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS);
        }
        repositorio.registarLoginValido(utilizador.id());
        return new PrincipalPad(utilizador.id(), utilizador.email(), utilizador.nomeCompleto(), null);
    }

    public boolean contaActiva(UUID utilizadorId) {
        return repositorio.contaActiva(utilizadorId);
    }

    public List<InstituicaoDisponivel> instituicoesDisponiveis(UUID utilizadorId) {
        return repositorio.instituicoesDisponiveis(utilizadorId);
    }

    public boolean podeActuarNaInstituicao(UUID utilizadorId, UUID instituicaoId) {
        return repositorio.podeActuarNaInstituicao(utilizadorId, instituicaoId);
    }

    public void validarEscolhaDeInstituicao(UUID utilizadorId, UUID instituicaoId) {
        if (!repositorio.podeActuarNaInstituicao(utilizadorId, instituicaoId)) {
            throw new AcessoNegadoException("Não tem acesso a essa instituição");
        }
    }

    public List<GrantedAuthority> autoridades(UUID utilizadorId, UUID instituicaoId) {
        Set<String> nomes = new LinkedHashSet<>(repositorio.autoridadesGlobais(utilizadorId));
        if (instituicaoId != null) {
            nomes.addAll(repositorio.autoridadesDaInstituicao(utilizadorId, instituicaoId));
        }
        List<GrantedAuthority> autoridades = new ArrayList<>();
        for (String nome : nomes) {
            autoridades.add(new SimpleGrantedAuthority(nome));
        }
        return autoridades;
    }
}

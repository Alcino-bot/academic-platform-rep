package mz.plataformaacademica.administracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class AdministradorInicialRunner implements ApplicationRunner {

    private static final Logger REGISTO = LoggerFactory.getLogger(AdministradorInicialRunner.class);

    private final ServicoDoAdministradorInicial servico;
    private final String email;
    private final String nome;
    private final String palavraPasse;

    public AdministradorInicialRunner(
            ServicoDoAdministradorInicial servico,
            @Value("${ADMIN_EMAIL:}") String email,
            @Value("${ADMIN_NOME:}") String nome,
            @Value("${ADMIN_PALAVRA_PASSE:}") String palavraPasse) {
        this.servico = servico;
        this.email = email;
        this.nome = nome;
        this.palavraPasse = palavraPasse;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        ServicoDoAdministradorInicial.Resultado resultado = servico.garantir(email, nome, palavraPasse);
        switch (resultado) {
            case CRIADO -> REGISTO.info("Administrador geral inicial criado.");
            case JA_EXISTE_COM_CONFIGURACAO -> REGISTO.warn(
                "Já existe um administrador geral; as variáveis ADMIN_* foram ignoradas. Retire-as da configuração.");
            case SEM_CONFIGURACAO -> REGISTO.warn(
                "Não existe nenhum administrador geral e ADMIN_EMAIL, ADMIN_NOME e ADMIN_PALAVRA_PASSE não estão definidas.");
            case JA_EXISTE -> {
            }
        }
    }
}

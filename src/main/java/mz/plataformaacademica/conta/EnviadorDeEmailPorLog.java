package mz.plataformaacademica.conta;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EnviadorDeEmailPorLog implements EnviadorDeEmail {

    private static final Logger REGISTO = LoggerFactory.getLogger(EnviadorDeEmailPorLog.class);
    private static final int MAXIMO_GUARDADAS = 50;

    public record MensagemEnviada(String destinatario, String assunto, String texto) {
    }

    private final List<MensagemEnviada> enviadas = new CopyOnWriteArrayList<>();

    @Override
    public void enviar(String destinatario, String assunto, String texto) {
        enviadas.add(new MensagemEnviada(destinatario, assunto, texto));
        while (enviadas.size() > MAXIMO_GUARDADAS) {
            enviadas.remove(0);
        }
        REGISTO.info("EMAIL (modo registo) para {} | {} | {}", destinatario, assunto, texto);
    }

    public List<MensagemEnviada> enviadas() {
        return List.copyOf(enviadas);
    }

    public void limpar() {
        enviadas.clear();
    }
}

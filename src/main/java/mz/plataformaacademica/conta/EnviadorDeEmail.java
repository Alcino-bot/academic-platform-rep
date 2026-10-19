package mz.plataformaacademica.conta;

public interface EnviadorDeEmail {

    void enviar(String destinatario, String assunto, String texto);
}

package mz.plataformaacademica.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import mz.plataformaacademica.comum.AcessoNegadoException;
import mz.plataformaacademica.comum.ConflitoException;
import mz.plataformaacademica.comum.CredenciaisInvalidasException;
import mz.plataformaacademica.comum.NaoEncontradoException;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ProblemDetail> credenciaisInvalidas(CredenciaisInvalidasException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ProblemDetail> acessoNegado(AcessoNegadoException e) {
        return resposta(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> naoEncontrado(NaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ProblemDetail> conflito(ConflitoException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ProblemDetail> regraDeNegocio(RegraDeNegocioException e) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    private ResponseEntity<ProblemDetail> resposta(HttpStatus estado, String detalhe) {
        return ResponseEntity.status(estado).body(ProblemDetail.forStatusAndDetail(estado, detalhe));
    }
}

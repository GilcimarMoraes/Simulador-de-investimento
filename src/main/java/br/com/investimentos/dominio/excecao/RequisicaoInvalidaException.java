package br.com.investimentos.dominio.excecao;

/** Entrada sintaticamente aceita, mas com valor invalido para o dominio (HTTP 400). */
public class RequisicaoInvalidaException extends RuntimeException {

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}

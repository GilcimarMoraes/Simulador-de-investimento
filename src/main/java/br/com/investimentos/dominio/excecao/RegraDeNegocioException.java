package br.com.investimentos.dominio.excecao;

/** Entrada valida, porem sem produto/condicao que atenda a regra de negocio (HTTP 422). */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}

package br.com.investimentos.dominio.excecao;

/** Usuario ou senha invalidos (HTTP 401), sem revelar qual dos dois falhou. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Usuário ou senha inválidos.");
    }
}

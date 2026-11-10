package br.com.investimentos.api.erro;

/** Erro de validacao de um campo especifico. */
public record CampoErro(String campo, String mensagem) {
}

package br.com.investimentos.servico;

import java.math.BigDecimal;

/** Entrada normalizada da simulacao, independente do formato do envelope HTTP. */
public record ComandoSimulacao(long clienteId, BigDecimal valor, int prazoMeses, String tipoProduto) {
}

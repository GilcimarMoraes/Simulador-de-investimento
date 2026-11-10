package br.com.investimentos.dominio;

import java.math.BigDecimal;
import java.time.Instant;

/** Simulacao persistida (tabela {@code simulacao}). */
public record Simulacao(
        long id,
        long clienteId,
        long produtoId,
        String produtoNome,
        String tipoProduto,
        BigDecimal valorInvestido,
        BigDecimal valorFinal,
        BigDecimal valorFinalLiquido,
        BigDecimal impostoRendimento,
        BigDecimal rentabilidadeEfetiva,
        int prazoMeses,
        PerfilRisco perfilCliente,
        Instant dataSimulacao) {
}

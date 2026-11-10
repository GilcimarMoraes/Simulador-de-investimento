package br.com.investimentos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Item do GET /simulacoes/por-produto-dia. */
public record SimulacaoPorProdutoDiaDTO(
        String produto,
        LocalDate data,
        long quantidadeSimulacoes,
        BigDecimal mediaValorFinal) {
}

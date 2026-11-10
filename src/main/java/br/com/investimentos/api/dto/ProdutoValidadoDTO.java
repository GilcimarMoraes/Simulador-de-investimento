package br.com.investimentos.api.dto;

import java.math.BigDecimal;

/** Produto escolhido para a simulação. */
public record ProdutoValidadoDTO(
        long id,
        String nome,
        String tipo,
        BigDecimal rentabilidade,
        String risco) {
}

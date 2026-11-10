package br.com.investimentos.api.dto;

import java.math.BigDecimal;

/**
 * Resultado financeiro. {@code valorFinal} e {@code rentabilidadeEfetiva} sao brutos, como no
 * envelope de referencia do desafio; os campos liquidos explicitam o efeito do IR.
 */
public record ResultadoSimulacaoDTO(
        BigDecimal valorFinal,
        BigDecimal rentabilidadeEfetiva,
        int prazoMeses,
        BigDecimal valorInvestido,
        BigDecimal valorFinalLiquido,
        BigDecimal impostoRendimento,
        BigDecimal rentabilidadeLiquida) {
}

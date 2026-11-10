package br.com.investimentos.dominio;

import java.math.BigDecimal;

/**
 * Resultado financeiro da simulacao.
 *
 * @param valorFinal                  valor bruto no vencimento (compativel com o envelope do desafio)
 * @param imposto                     IR retido sobre o rendimento
 * @param valorFinalLiquido           valor apos IR
 * @param rentabilidadeEfetiva        rentabilidade bruta efetiva no periodo
 * @param rentabilidadeLiquidaEfetiva rentabilidade liquida efetiva no periodo
 * @param aliquotaIr                  aliquota de IR aplicada
 */
public record ResultadoCalculo(
        BigDecimal valorFinal,
        BigDecimal imposto,
        BigDecimal valorFinalLiquido,
        BigDecimal rentabilidadeEfetiva,
        BigDecimal rentabilidadeLiquidaEfetiva,
        BigDecimal aliquotaIr) {
}

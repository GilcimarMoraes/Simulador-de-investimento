package br.com.investimentos.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Movimentacao financeira (aporte/resgate) usada no criterio de frequencia. */
public record Movimentacao(
        long id,
        long clienteId,
        TipoMovimentacao tipo,
        BigDecimal valor,
        LocalDate data) {
}

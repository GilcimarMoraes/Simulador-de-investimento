package br.com.investimentos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Item do GET /investimentos/{clienteId}. */
public record InvestimentoDTO(
        long id,
        String tipo,
        BigDecimal valor,
        BigDecimal rentabilidade,
        LocalDate data) {
}

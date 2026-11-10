package br.com.investimentos.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Item do GET /simulacoes. */
public record SimulacaoHistoricoDTO(
        long id,
        long clienteId,
        String produto,
        BigDecimal valorInvestido,
        BigDecimal valorFinal,
        int prazoMeses,
        Instant dataSimulacao) {
}

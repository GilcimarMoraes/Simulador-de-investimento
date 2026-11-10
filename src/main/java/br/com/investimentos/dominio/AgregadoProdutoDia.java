package br.com.investimentos.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Agregado diario de simulacoes por produto. */
public record AgregadoProdutoDia(
        String produto,
        LocalDate data,
        long quantidadeSimulacoes,
        BigDecimal mediaValorFinal) {
}

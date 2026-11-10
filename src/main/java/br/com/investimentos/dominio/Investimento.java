package br.com.investimentos.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Posicao de investimento do cliente, usada como entrada do motor de perfil. */
public record Investimento(
        long id,
        long clienteId,
        String tipo,
        BigDecimal valor,
        BigDecimal rentabilidadeAnual,
        Liquidez liquidez,
        LocalDate data) {
}

package br.com.investimentos.api.dto;

import java.math.BigDecimal;

/** Item do GET /produtos-recomendados/{perfil}. */
public record ProdutoRecomendadoDTO(
        long id,
        String nome,
        String tipo,
        BigDecimal rentabilidade,
        String risco,
        String liquidez,
        int prazoMinimoMeses,
        Integer prazoMaximoMeses,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo) {
}

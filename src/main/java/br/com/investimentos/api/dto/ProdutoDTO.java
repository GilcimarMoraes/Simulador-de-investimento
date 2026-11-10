package br.com.investimentos.api.dto;

import java.math.BigDecimal;

/** Item do catalogo GET /produtos. */
public record ProdutoDTO(
        long id,
        String nome,
        String tipo,
        BigDecimal rentabilidade,
        String risco,
        String liquidez,
        int prazoMinimoMeses,
        Integer prazoMaximoMeses,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo,
        boolean isentoIr,
        int carenciaMeses,
        boolean ativo,
        String descricao) {
}

package br.com.investimentos.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regras unicas de arredondamento monetario e de conversao entre a representacao de dominio
 * (BigDecimal) e a representacao persistida (centavos inteiros), evitando erro de ponto flutuante.
 */
public final class Dinheiro {

    public static final int ESCALA_MONETARIA = 2;
    public static final int ESCALA_TAXA = 8;

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    public static BigDecimal deCentavos(long centavos) {
        return BigDecimal.valueOf(centavos, 2);
    }

    public static long paraCentavos(BigDecimal valor) {
        return valor.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }

    /** Converte basis points em taxa decimal: 1200 -> 0.1200 (12% ao ano). */
    public static BigDecimal taxaDeBasisPoints(int basisPoints) {
        return BigDecimal.valueOf(basisPoints, 4);
    }

    public static int basisPointsDe(BigDecimal taxa) {
        return taxa.setScale(4, RoundingMode.HALF_UP).movePointRight(4).intValueExact();
    }

    /** Remove zeros a direita preservando a magnitude (0.120000 -> 0.12). */
    public static BigDecimal taxaCompacta(BigDecimal taxa) {
        BigDecimal compacta = taxa.setScale(ESCALA_TAXA, RoundingMode.HALF_UP).stripTrailingZeros();
        return compacta.scale() < 0 ? compacta.setScale(0) : compacta;
    }
}

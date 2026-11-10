package br.com.investimentos.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DinheiroTest {

    @Test
    void converteCentavos() {
        assertEquals(0, new BigDecimal("11200.00").compareTo(Dinheiro.deCentavos(1_120_000)));
        assertEquals(1_120_000L, Dinheiro.paraCentavos(new BigDecimal("11200.004")));
        assertEquals(1_120_001L, Dinheiro.paraCentavos(new BigDecimal("11200.005")));
    }

    @Test
    void converteBasisPoints() {
        assertEquals(0, new BigDecimal("0.12").compareTo(Dinheiro.taxaDeBasisPoints(1200)));
        assertEquals(1200, Dinheiro.basisPointsDe(new BigDecimal("0.12")));
        assertEquals(1050, Dinheiro.basisPointsDe(new BigDecimal("0.105")));
    }

    @Test
    void compactaTaxaSemPerderMagnitude() {
        assertEquals("0.12", Dinheiro.taxaCompacta(new BigDecimal("0.120000")).toPlainString());
        assertEquals("0.105", Dinheiro.taxaCompacta(new BigDecimal("0.1050")).toPlainString());
        assertEquals("1", Dinheiro.taxaCompacta(new BigDecimal("1.000000")).toPlainString());
    }
}

package br.com.investimentos.servico;

import br.com.investimentos.dominio.Liquidez;
import br.com.investimentos.dominio.Produto;
import br.com.investimentos.dominio.ResultadoCalculo;
import br.com.investimentos.dominio.Risco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculadoraInvestimentoTest {

    private static Produto produto(String tipo, String taxaAnual, boolean isentoIr) {
        return new Produto(1L, "Produto Teste", tipo, new BigDecimal(taxaAnual), Risco.BAIXO,
                Liquidez.DIARIA, 1, 120, new BigDecimal("100.00"), null, isentoIr, 0, true, null);
    }

    private static void assertValor(String esperado, BigDecimal obtido) {
        assertEquals(0, new BigDecimal(esperado).compareTo(obtido),
                () -> "esperado " + esperado + " mas foi " + obtido);
    }

    @Test
    @DisplayName("CDB 12% por 12 meses reproduz o envelope de referencia do desafio")
    void cdbDozeMeses() {
        ResultadoCalculo resultado = CalculadoraInvestimento.calcular(
                produto("CDB", "0.12", false), new BigDecimal("10000.00"), 12);

        assertValor("11200.00", resultado.valorFinal());
        assertValor("240.00", resultado.imposto());
        assertValor("10960.00", resultado.valorFinalLiquido());
        assertValor("0.12", resultado.rentabilidadeEfetiva());
        assertValor("0.096", resultado.rentabilidadeLiquidaEfetiva());
        assertValor("0.20", resultado.aliquotaIr());
    }

    @Test
    @DisplayName("CDB de 6 meses aplica aliquota de 22,5%")
    void cdbSeisMeses() {
        ResultadoCalculo resultado = CalculadoraInvestimento.calcular(
                produto("CDB", "0.12", false), new BigDecimal("10000.00"), 6);

        assertValor("10583.01", resultado.valorFinal());
        assertValor("131.18", resultado.imposto());
        assertValor("10451.83", resultado.valorFinalLiquido());
        assertEquals(0.05830052d, resultado.rentabilidadeEfetiva().doubleValue(), 1e-9);
    }

    @Test
    @DisplayName("IR regressivo: 17,5% entre 361 e 720 dias e 15% acima de 720 dias")
    void irRegressivo() {
        ResultadoCalculo doisAnos = CalculadoraInvestimento.calcular(
                produto("CDB", "0.12", false), new BigDecimal("10000.00"), 24);
        assertValor("0.175", doisAnos.aliquotaIr());
        assertValor("12544.00", doisAnos.valorFinal());

        ResultadoCalculo tresAnos = CalculadoraInvestimento.calcular(
                produto("CDB", "0.12", false), new BigDecimal("10000.00"), 36);
        assertValor("0.15", tresAnos.aliquotaIr());
        assertValor("14049.28", tresAnos.valorFinal());
    }

    @Test
    @DisplayName("LCI/LCA isentas nao retêm imposto")
    void isencaoDeIr() {
        ResultadoCalculo resultado = CalculadoraInvestimento.calcular(
                produto("LCI", "0.105", true), new BigDecimal("10000.00"), 12);

        assertValor("11050.00", resultado.valorFinal());
        assertValor("0.00", resultado.imposto());
        assertValor("11050.00", resultado.valorFinalLiquido());
        assertEquals(0, resultado.aliquotaIr().signum());
    }

    @Test
    @DisplayName("Fundo usa 20% ate 180 dias e 15% acima")
    void tributacaoDeFundos() {
        ResultadoCalculo curto = CalculadoraInvestimento.calcular(
                produto("FUNDO", "0.18", false), new BigDecimal("10000.00"), 6);
        assertValor("0.20", curto.aliquotaIr());
        assertValor("10862.78", curto.valorFinal());
        assertValor("172.56", curto.imposto());

        ResultadoCalculo longo = CalculadoraInvestimento.calcular(
                produto("FUNDO", "0.18", false), new BigDecimal("10000.00"), 12);
        assertValor("0.15", longo.aliquotaIr());
        assertValor("11800.00", longo.valorFinal());
        assertValor("270.00", longo.imposto());
    }

    @Test
    @DisplayName("Entradas invalidas sao rejeitadas")
    void entradasInvalidas() {
        Produto cdb = produto("CDB", "0.12", false);
        assertThrows(IllegalArgumentException.class,
                () -> CalculadoraInvestimento.calcular(cdb, BigDecimal.ZERO, 12));
        assertThrows(IllegalArgumentException.class,
                () -> CalculadoraInvestimento.calcular(cdb, new BigDecimal("1000"), 0));
    }

    @Test
    @DisplayName("Tabela de aliquotas por prazo em dias")
    void tabelaDeAliquotas() {
        Produto cdb = produto("CDB", "0.12", false);
        assertValor("0.225", CalculadoraInvestimento.aliquotaIr(cdb, 1));
        assertValor("0.225", CalculadoraInvestimento.aliquotaIr(cdb, 6));
        assertValor("0.20", CalculadoraInvestimento.aliquotaIr(cdb, 7));
        assertValor("0.20", CalculadoraInvestimento.aliquotaIr(cdb, 12));
        assertValor("0.175", CalculadoraInvestimento.aliquotaIr(cdb, 13));
        assertValor("0.175", CalculadoraInvestimento.aliquotaIr(cdb, 24));
        assertValor("0.15", CalculadoraInvestimento.aliquotaIr(cdb, 25));
    }
}

package br.com.investimentos.dominio;

import java.math.BigDecimal;

/**
 * Produto de investimento parametrizado no banco (tabela {@code produto}).
 * Os limites de valor e prazo sao usados na validacao e no filtro da simulacao.
 */
public record Produto(
        long id,
        String nome,
        String tipo,
        BigDecimal rentabilidadeAnual,
        Risco risco,
        Liquidez liquidez,
        int prazoMinimoMeses,
        Integer prazoMaximoMeses,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo,
        boolean isentoIr,
        int carenciaMeses,
        boolean ativo,
        String descricao) {

    public boolean aceitaValor(BigDecimal valor) {
        boolean acimaDoMinimo = valor.compareTo(valorMinimo) >= 0;
        boolean abaixoDoMaximo = valorMaximo == null || valor.compareTo(valorMaximo) <= 0;
        return acimaDoMinimo && abaixoDoMaximo;
    }

    public boolean aceitaPrazo(int prazoMeses) {
        boolean acimaDoMinimo = prazoMeses >= prazoMinimoMeses;
        boolean abaixoDoMaximo = prazoMaximoMeses == null || prazoMeses <= prazoMaximoMeses;
        return acimaDoMinimo && abaixoDoMaximo;
    }

    public boolean aceita(BigDecimal valor, int prazoMeses) {
        return aceitaValor(valor) && aceitaPrazo(prazoMeses);
    }
}

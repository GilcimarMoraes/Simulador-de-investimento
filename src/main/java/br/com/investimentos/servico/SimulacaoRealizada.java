package br.com.investimentos.servico;

import br.com.investimentos.dominio.Produto;
import br.com.investimentos.dominio.ResultadoCalculo;
import br.com.investimentos.dominio.Simulacao;

import java.util.List;

/** Resultado completo de uma simulacao: dados persistidos, produto escolhido e alertas. */
public record SimulacaoRealizada(
        Simulacao simulacao,
        Produto produto,
        ResultadoCalculo calculo,
        List<String> alertas) {
}

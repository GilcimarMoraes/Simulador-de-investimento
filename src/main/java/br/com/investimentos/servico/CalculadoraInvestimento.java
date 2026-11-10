package br.com.investimentos.servico;

import br.com.investimentos.dominio.Dinheiro;
import br.com.investimentos.dominio.Produto;
import br.com.investimentos.dominio.ResultadoCalculo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Calculos financeiros das simulacoes.
 *
 * <p>Premissas (documentadas no README):</p>
 * <ul>
 *   <li>Juros compostos com capitalizacao anual: {@code VF = VP * (1 + taxa)^(meses/12)}.</li>
 *   <li>IR regressivo para renda fixa (CDB/Tesouro): 22,5% ate 180 dias, 20% ate 360,
 *       17,5% ate 720 e 15% acima.</li>
 *   <li>LCI/LCA sao isentas de IR (flag {@code isento_ir} do produto).</li>
 *   <li>Fundos: 20% ate 180 dias (curto prazo) e 15% acima (longo prazo).</li>
 *   <li>IOF nao incide porque o prazo minimo aceito pela API e de 1 mes (30 dias).</li>
 * </ul>
 */
public final class CalculadoraInvestimento {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private CalculadoraInvestimento() {
    }

    public static ResultadoCalculo calcular(Produto produto, BigDecimal valor, int prazoMeses) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor do investimento deve ser positivo");
        }
        if (prazoMeses <= 0) {
            throw new IllegalArgumentException("Prazo em meses deve ser positivo");
        }

        BigDecimal fator = fatorAcumulado(produto.rentabilidadeAnual(), prazoMeses);
        BigDecimal valorFinal = Dinheiro.arredondar(valor.multiply(fator, MC));

        BigDecimal lucro = valorFinal.subtract(valor).max(ZERO);
        BigDecimal aliquota = aliquotaIr(produto, prazoMeses);
        BigDecimal imposto = Dinheiro.arredondar(lucro.multiply(aliquota, MC));
        BigDecimal valorFinalLiquido = valorFinal.subtract(imposto);

        BigDecimal efetiva = fator.subtract(BigDecimal.ONE)
                .setScale(Dinheiro.ESCALA_TAXA, RoundingMode.HALF_UP);
        BigDecimal efetivaLiquida = valorFinalLiquido.divide(valor, Dinheiro.ESCALA_TAXA, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .setScale(Dinheiro.ESCALA_TAXA, RoundingMode.HALF_UP);

        return new ResultadoCalculo(
                valorFinal,
                imposto,
                Dinheiro.arredondar(valorFinalLiquido),
                Dinheiro.taxaCompacta(efetiva),
                Dinheiro.taxaCompacta(efetivaLiquida),
                aliquota);
    }

    /** Fator de acumulacao composta para o prazo informado em meses. */
    public static BigDecimal fatorAcumulado(BigDecimal taxaAnual, int prazoMeses) {
        double fator = Math.pow(1.0d + taxaAnual.doubleValue(), prazoMeses / 12.0d);
        return BigDecimal.valueOf(fator);
    }

    /**
     * Aliquota de IR aplicavel ao rendimento, conforme o tipo do produto e o prazo.
     * Visivel para testes e para a documentacao da regra.
     */
    public static BigDecimal aliquotaIr(Produto produto, int prazoMeses) {
        if (produto.isentoIr()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        int dias = prazoMeses * 30;
        if (produto.tipo() != null && produto.tipo().equalsIgnoreCase("FUNDO")) {
            return dias <= 180
                    ? new BigDecimal("0.2000")
                    : new BigDecimal("0.1500");
        }
        if (dias <= 180) {
            return new BigDecimal("0.2250");
        }
        if (dias <= 360) {
            return new BigDecimal("0.2000");
        }
        if (dias <= 720) {
            return new BigDecimal("0.1750");
        }
        return new BigDecimal("0.1500");
    }

}

package br.com.investimentos.servico;

import br.com.investimentos.dominio.AnalisePerfil;
import br.com.investimentos.dominio.Dinheiro;
import br.com.investimentos.dominio.FatorPerfil;
import br.com.investimentos.dominio.Investimento;
import br.com.investimentos.dominio.Liquidez;
import br.com.investimentos.dominio.PerfilRisco;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Motor de pontuacao do perfil de risco.
 *
 * <p>E um calculo puro (sem banco, sem relogio) para ser testavel e auditavel. A pontuacao
 * final e a soma de quatro criterios, totalizando no maximo 100 pontos:</p>
 *
 * <pre>
 *   Volume investido ......................... 0 a 30
 *   Frequencia de movimentacoes (180 dias) ... 0 a 25
 *   Preferencia por rentabilidade (iliquidez)  0 a 30
 *   Apetite por retorno (rentabilidade media) 0 a 15
 * </pre>
 *
 * <p>Quanto mais volume, mais movimentacao, mais concentracao em ativos iliquidos e maior a
 * rentabilidade media buscada, mais agressivo tende a ser o perfil.</p>
 */
public final class MotorPerfilRisco {

    public static final int PESO_VOLUME = 30;
    public static final int PESO_FREQUENCIA = 25;
    public static final int PESO_ILIQUIDEZ = 30;
    public static final int PESO_RENTABILIDADE = 15;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private MotorPerfilRisco() {
    }

    /**
     * Calcula o perfil a partir das posicoes e do volume de movimentacoes dos ultimos 180 dias.
     *
     * @param clienteId        identificador do cliente
     * @param investimentos    posicoes atuais (pode ser vazio)
     * @param movimentacoes180 quantidade de aportes/resgates nos ultimos 180 dias
     */
    public static AnalisePerfil analisar(long clienteId, List<Investimento> investimentos,
                                         int movimentacoes180) {
        List<Investimento> posicoes = investimentos == null ? List.of() : investimentos;

        BigDecimal total = posicoes.stream()
                .map(Investimento::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal iliquido = posicoes.stream()
                .filter(i -> i.liquidez() == Liquidez.VENCIMENTO)
                .map(Investimento::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentualIliquido = total.signum() == 0
                ? BigDecimal.ZERO
                : iliquido.divide(total, 6, RoundingMode.HALF_UP);

        BigDecimal rentabilidadeMedia = total.signum() == 0
                ? null
                : posicoes.stream()
                        .map(i -> i.valor().multiply(i.rentabilidadeAnual()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(total, 6, RoundingMode.HALF_UP);

        List<FatorPerfil> fatores = new ArrayList<>(4);

        int pontosVolume = pontosVolume(total);
        fatores.add(new FatorPerfil("Volume investido", pontosVolume, PESO_VOLUME,
                "Patrimônio de %s em %d posição(ões).".formatted(moeda(total), posicoes.size())));

        int pontosFrequencia = pontosFrequencia(movimentacoes180);
        fatores.add(new FatorPerfil("Frequência de movimentações", pontosFrequencia, PESO_FREQUENCIA,
                "%d movimentação(ões) nos últimos 180 dias.".formatted(movimentacoes180)));

        int pontosIliquidez = pontosIliquidez(percentualIliquido);
        fatores.add(new FatorPerfil("Preferência por rentabilidade", pontosIliquidez, PESO_ILIQUIDEZ,
                "%s do patrimônio em ativos sem liquidez diária.".formatted(percentual(percentualIliquido))));

        int pontosRentabilidade = pontosRentabilidade(rentabilidadeMedia);
        fatores.add(new FatorPerfil("Apetite por retorno", pontosRentabilidade, PESO_RENTABILIDADE,
                rentabilidadeMedia == null
                        ? "Sem histórico de rentabilidade; perfil assume postura conservadora."
                        : "Rentabilidade média ponderada de %s ao ano.".formatted(percentual(rentabilidadeMedia))));

        int pontuacao = Math.min(100, pontosVolume + pontosFrequencia + pontosIliquidez + pontosRentabilidade);
        PerfilRisco perfil = PerfilRisco.porPontuacao(pontuacao);

        BigDecimal percentualLiquidez = BigDecimal.ONE.subtract(percentualIliquido)
                .setScale(4, RoundingMode.HALF_UP);

        return new AnalisePerfil(
                clienteId,
                perfil,
                pontuacao,
                perfil.texto(),
                List.copyOf(fatores),
                Dinheiro.arredondar(total),
                percentualLiquidez,
                movimentacoes180,
                rentabilidadeMedia == null
                        ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                        : rentabilidadeMedia.setScale(4, RoundingMode.HALF_UP));
    }

    static int pontosVolume(BigDecimal total) {
        BigDecimal valor = total == null ? BigDecimal.ZERO : total;
        if (valor.compareTo(new BigDecimal("10000")) < 0) {
            return 4;
        }
        if (valor.compareTo(new BigDecimal("50000")) < 0) {
            return 12;
        }
        if (valor.compareTo(new BigDecimal("200000")) < 0) {
            return 20;
        }
        if (valor.compareTo(new BigDecimal("1000000")) < 0) {
            return 26;
        }
        return 30;
    }

    static int pontosFrequencia(int movimentacoes) {
        if (movimentacoes <= 0) {
            return 2;
        }
        if (movimentacoes <= 2) {
            return 8;
        }
        if (movimentacoes <= 6) {
            return 14;
        }
        if (movimentacoes <= 12) {
            return 20;
        }
        return 25;
    }

    static int pontosIliquidez(BigDecimal percentualIliquido) {
        BigDecimal percentual = percentualIliquido == null ? BigDecimal.ZERO : percentualIliquido;
        BigDecimal normalizado = percentual.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return normalizado.multiply(BigDecimal.valueOf(PESO_ILIQUIDEZ))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    static int pontosRentabilidade(BigDecimal rentabilidadeMedia) {
        if (rentabilidadeMedia == null) {
            return 2;
        }
        if (rentabilidadeMedia.compareTo(new BigDecimal("0.08")) < 0) {
            return 2;
        }
        if (rentabilidadeMedia.compareTo(new BigDecimal("0.10")) < 0) {
            return 6;
        }
        if (rentabilidadeMedia.compareTo(new BigDecimal("0.12")) < 0) {
            return 10;
        }
        if (rentabilidadeMedia.compareTo(new BigDecimal("0.15")) < 0) {
            return 13;
        }
        return 15;
    }

    private static String moeda(BigDecimal valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }

    private static String percentual(BigDecimal valor) {
        return String.format(PT_BR, "%.1f%%", valor.multiply(BigDecimal.valueOf(100)));
    }
}

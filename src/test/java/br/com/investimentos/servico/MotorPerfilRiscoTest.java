package br.com.investimentos.servico;

import br.com.investimentos.dominio.AnalisePerfil;
import br.com.investimentos.dominio.Investimento;
import br.com.investimentos.dominio.Liquidez;
import br.com.investimentos.dominio.PerfilRisco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotorPerfilRiscoTest {

    private static long proximoId = 1;

    private static Investimento investimento(String valor, String taxa, Liquidez liquidez) {
        return new Investimento(proximoId++, 123L, "TESTE", new BigDecimal(valor),
                new BigDecimal(taxa), liquidez, java.time.LocalDate.now());
    }

    @Test
    @DisplayName("Cliente 123 de demonstracao resulta em 65 pontos e perfil Moderado")
    void perfilModerado() {
        List<Investimento> carteira = List.of(
                investimento("80000", "0.12", Liquidez.DIARIA),
                investimento("100000", "0.11", Liquidez.DIARIA),
                investimento("120000", "0.18", Liquidez.VENCIMENTO),
                investimento("5000", "0.12", Liquidez.DIARIA),
                investimento("3000", "0.18", Liquidez.VENCIMENTO));

        AnalisePerfil analise = MotorPerfilRisco.analisar(123L, carteira, 5);

        assertEquals(65, analise.pontuacao());
        assertEquals(PerfilRisco.MODERADO, analise.perfil());
        assertEquals(0, new BigDecimal("308000.00").compareTo(analise.totalInvestido()));
        assertEquals(0, new BigDecimal("0.6006").compareTo(analise.percentualLiquidez()));
        assertEquals(4, analise.fatores().size());
        assertEquals(26, analise.fatores().get(0).pontos());
        assertEquals(14, analise.fatores().get(1).pontos());
        assertEquals(12, analise.fatores().get(2).pontos());
        assertEquals(13, analise.fatores().get(3).pontos());
        assertEquals(65, analise.fatores().stream().mapToInt(f -> f.pontos()).sum());
    }

    @Test
    @DisplayName("Carteira pequena, liquida e com poucas movimentacoes gera perfil Conservador")
    void perfilConservador() {
        List<Investimento> carteira = List.of(
                investimento("20000", "0.10", Liquidez.DIARIA),
                investimento("10000", "0.11", Liquidez.DIARIA));

        AnalisePerfil analise = MotorPerfilRisco.analisar(1L, carteira, 1);

        assertEquals(30, analise.pontuacao());
        assertEquals(PerfilRisco.CONSERVADOR, analise.perfil());
        assertEquals(0, new BigDecimal("100.0000").compareTo(
                analise.percentualLiquidez().multiply(new BigDecimal("100"))));
    }

    @Test
    @DisplayName("Volume alto, muitas movimentacoes e ativos iliquidos geram perfil Agressivo")
    void perfilAgressivo() {
        List<Investimento> carteira = List.of(
                investimento("500000", "0.18", Liquidez.VENCIMENTO),
                investimento("300000", "0.22", Liquidez.VENCIMENTO));

        AnalisePerfil analise = MotorPerfilRisco.analisar(2L, carteira, 15);

        assertEquals(96, analise.pontuacao());
        assertEquals(PerfilRisco.AGRESSIVO, analise.perfil());
    }

    @Test
    @DisplayName("Cliente sem historico assume postura conservadora")
    void semHistorico() {
        AnalisePerfil analise = MotorPerfilRisco.analisar(3L, List.of(), 0);

        assertEquals(8, analise.pontuacao());
        assertEquals(PerfilRisco.CONSERVADOR, analise.perfil());
        assertTrue(analise.fatores().get(3).detalhe().contains("Sem histórico"));
    }

    @Test
    @DisplayName("Faixas de pontuacao respeitam os limites 39/40 e 69/70")
    void faixasDePontuacao() {
        assertEquals(PerfilRisco.CONSERVADOR, PerfilRisco.porPontuacao(0));
        assertEquals(PerfilRisco.CONSERVADOR, PerfilRisco.porPontuacao(39));
        assertEquals(PerfilRisco.MODERADO, PerfilRisco.porPontuacao(40));
        assertEquals(PerfilRisco.MODERADO, PerfilRisco.porPontuacao(69));
        assertEquals(PerfilRisco.AGRESSIVO, PerfilRisco.porPontuacao(70));
        assertEquals(PerfilRisco.AGRESSIVO, PerfilRisco.porPontuacao(100));
    }

    @Test
    @DisplayName("Tabelas de pontuacao por criterio")
    void tabelasDePontuacao() {
        assertEquals(4, MotorPerfilRisco.pontosVolume(new BigDecimal("9999.99")));
        assertEquals(12, MotorPerfilRisco.pontosVolume(new BigDecimal("10000")));
        assertEquals(20, MotorPerfilRisco.pontosVolume(new BigDecimal("199999.99")));
        assertEquals(26, MotorPerfilRisco.pontosVolume(new BigDecimal("200000")));
        assertEquals(26, MotorPerfilRisco.pontosVolume(new BigDecimal("1000000").subtract(BigDecimal.ONE)));
        assertEquals(30, MotorPerfilRisco.pontosVolume(new BigDecimal("1000000")));

        assertEquals(2, MotorPerfilRisco.pontosFrequencia(0));
        assertEquals(8, MotorPerfilRisco.pontosFrequencia(2));
        assertEquals(14, MotorPerfilRisco.pontosFrequencia(6));
        assertEquals(20, MotorPerfilRisco.pontosFrequencia(12));
        assertEquals(25, MotorPerfilRisco.pontosFrequencia(13));

        assertEquals(0, MotorPerfilRisco.pontosIliquidez(BigDecimal.ZERO));
        assertEquals(15, MotorPerfilRisco.pontosIliquidez(new BigDecimal("0.5")));
        assertEquals(30, MotorPerfilRisco.pontosIliquidez(BigDecimal.ONE));

        assertEquals(2, MotorPerfilRisco.pontosRentabilidade(null));
        assertEquals(2, MotorPerfilRisco.pontosRentabilidade(new BigDecimal("0.07")));
        assertEquals(6, MotorPerfilRisco.pontosRentabilidade(new BigDecimal("0.09")));
        assertEquals(10, MotorPerfilRisco.pontosRentabilidade(new BigDecimal("0.11")));
        assertEquals(13, MotorPerfilRisco.pontosRentabilidade(new BigDecimal("0.14")));
        assertEquals(15, MotorPerfilRisco.pontosRentabilidade(new BigDecimal("0.15")));
    }
}

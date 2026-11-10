package br.com.investimentos.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado completo do motor de perfil: a classificacao e os dados que a justificam.
 */
public record AnalisePerfil(
        long clienteId,
        PerfilRisco perfil,
        int pontuacao,
        String descricao,
        List<FatorPerfil> fatores,
        BigDecimal totalInvestido,
        BigDecimal percentualLiquidez,
        int movimentacoes180Dias,
        BigDecimal rentabilidadeMedia) {
}

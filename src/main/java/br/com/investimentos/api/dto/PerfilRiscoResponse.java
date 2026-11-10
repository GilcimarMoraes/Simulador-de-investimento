package br.com.investimentos.api.dto;

import java.math.BigDecimal;
import java.util.List;

/** Envelope de resposta do GET /perfil-risco/{clienteId}. */
public record PerfilRiscoResponse(
        long clienteId,
        String perfil,
        int pontuacao,
        String descricao,
        List<FatorPerfilDTO> fatores,
        BigDecimal totalInvestido,
        BigDecimal percentualLiquidez,
        int movimentacoes180Dias,
        BigDecimal rentabilidadeMedia) {
}

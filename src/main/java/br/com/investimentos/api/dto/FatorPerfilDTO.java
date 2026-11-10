package br.com.investimentos.api.dto;

/** Parcela auditavel do cálculo de perfil. */
public record FatorPerfilDTO(String criterio, int pontos, int maximo, String detalhe) {
}

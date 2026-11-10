package br.com.investimentos.dominio;

/**
 * Parcela da pontuacao do perfil de risco. Exposta na API para tornar o calculo auditavel.
 *
 * @param criterio nome do criterio avaliado
 * @param pontos   pontos obtidos
 * @param maximo   pontos maximos possiveis do criterio
 * @param detalhe  explicacao legivel do calculo
 */
public record FatorPerfil(String criterio, int pontos, int maximo, String detalhe) {
}

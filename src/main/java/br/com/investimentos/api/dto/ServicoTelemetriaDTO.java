package br.com.investimentos.api.dto;

/** Estatisticas de um servico no periodo consultado. */
public record ServicoTelemetriaDTO(
        String nome,
        long quantidadeChamadas,
        long mediaTempoRespostaMs,
        long p95TempoRespostaMs,
        long maxTempoRespostaMs,
        long erros) {
}

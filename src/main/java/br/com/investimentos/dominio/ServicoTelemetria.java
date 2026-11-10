package br.com.investimentos.dominio;

/** Agregado de telemetria por servico. */
public record ServicoTelemetria(
        String nome,
        long quantidadeChamadas,
        long mediaTempoRespostaMs,
        long p95TempoRespostaMs,
        long maxTempoRespostaMs,
        long erros) {
}

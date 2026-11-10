package br.com.investimentos.dominio;

import java.time.LocalDate;
import java.util.List;

/** Resposta consolidada de telemetria. */
public record TelemetriaResumo(
        List<ServicoTelemetria> servicos,
        LocalDate inicio,
        LocalDate fim) {
}

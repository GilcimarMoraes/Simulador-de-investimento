package br.com.investimentos.infra;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Conversao entre os tipos temporais do dominio (Instant/LocalDate) e os tipos do PostgreSQL.
 *
 * <p>Todo instante e gravado como {@code TIMESTAMPTZ} em UTC; datas de negocio usam {@code DATE}.</p>
 */
public final class Datas {

    private Datas() {
    }

    public static OffsetDateTime paraTimestamp(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }

    public static Instant paraInstante(OffsetDateTime timestamp) {
        return timestamp.toInstant();
    }

    /** Primeiro instante do dia (UTC), usado como limite inferior inclusivo de intervalos. */
    public static OffsetDateTime inicioDoDia(LocalDate dia) {
        return dia.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    }
}

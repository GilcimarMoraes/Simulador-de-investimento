package br.com.investimentos.infra;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Formato unico de data/hora persistido no SQLite: ISO-8601 UTC com milissegundos.
 *
 * <p>O formato tem tamanho fixo, o que torna a ordenacao e os filtros por intervalo
 * corretos em comparacoes lexicograficas no SQL (inclusive para chaves de indice).</p>
 */
public final class Datas {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private Datas() {
    }

    public static String formatar(Instant instante) {
        return FORMATO.format(instante);
    }

    public static Instant ler(String valor) {
        return Instant.parse(valor);
    }

    /** Primeiro instante do dia (UTC), usado como limite inferior inclusivo. */
    public static String inicioDoDia(LocalDate dia) {
        return FORMATO.format(dia.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    /** Primeiro instante do dia seguinte (UTC), usado como limite superior exclusivo. */
    public static String fimDoDiaExclusivo(LocalDate dia) {
        return FORMATO.format(dia.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    public static LocalDate diaDe(String dataHora) {
        return LocalDate.parse(dataHora.substring(0, 10));
    }
}

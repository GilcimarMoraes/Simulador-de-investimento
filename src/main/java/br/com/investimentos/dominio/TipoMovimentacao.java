package br.com.investimentos.dominio;

import java.util.Arrays;

/** Natureza da movimentacao financeira do cliente. */
public enum TipoMovimentacao {
    APORTE,
    RESGATE;

    public static TipoMovimentacao de(String valor) {
        String normalizado = Risco.Texto.normalizar(valor);
        return Arrays.stream(values())
                .filter(t -> t.name().equals(normalizado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Movimentacao desconhecida: " + valor));
    }
}

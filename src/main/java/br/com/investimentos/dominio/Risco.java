package br.com.investimentos.dominio;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

/**
 * Grau de risco de um produto. A ordem (BAIXO &lt; MEDIO &lt; ALTO) e usada em comparacoes
 * de compatibilidade com o perfil do cliente.
 */
public enum Risco {
    BAIXO("Baixo", 1),
    MEDIO("Médio", 2),
    ALTO("Alto", 3);

    private final String descricao;
    private final int nivel;

    Risco(String descricao, int nivel) {
        this.descricao = descricao;
        this.nivel = nivel;
    }

    public String descricao() {
        return descricao;
    }

    public int nivel() {
        return nivel;
    }

    public boolean superiorA(Risco outro) {
        return this.nivel > outro.nivel;
    }

    /** Converte o valor persistido (ex.: "MEDIO") de forma tolerante a caixa e acentos. */
    public static Risco de(String valor) {
        String normalizado = Texto.normalizar(valor);
        return Arrays.stream(values())
                .filter(r -> Texto.normalizar(r.name()).equals(normalizado)
                        || Texto.normalizar(r.descricao).equals(normalizado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Risco desconhecido: " + valor));
    }

    /** Utilitario de normalizacao de texto para comparacoes tolerantes. */
    public static final class Texto {
        private Texto() {
        }

        public static String normalizar(String valor) {
            if (valor == null) {
                return "";
            }
            String semAcento = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");
            return semAcento.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        }
    }
}

package br.com.investimentos.dominio;

import java.util.Arrays;

/**
 * Perfil de risco calculado dinamicamente a partir do comportamento financeiro do cliente.
 *
 * <p>As faixas de pontuacao sao fixas e documentadas para garantir previsibilidade do motor:</p>
 * <ul>
 *   <li>0 a 39 pontos: Conservador</li>
 *   <li>40 a 69 pontos: Moderado</li>
 *   <li>70 a 100 pontos: Agressivo</li>
 * </ul>
 */
public enum PerfilRisco {
    CONSERVADOR("Conservador", 0, 39,
            "Prioriza segurança e liquidez, com baixa tolerância a oscilações."),
    MODERADO("Moderado", 40, 69,
            "Perfil equilibrado entre segurança e rentabilidade."),
    AGRESSIVO("Agressivo", 70, 100,
            "Busca maior rentabilidade e aceita oscilações e risco mais elevado.");

    private final String descricao;
    private final int pontuacaoMinima;
    private final int pontuacaoMaxima;
    private final String texto;

    PerfilRisco(String descricao, int pontuacaoMinima, int pontuacaoMaxima, String texto) {
        this.descricao = descricao;
        this.pontuacaoMinima = pontuacaoMinima;
        this.pontuacaoMaxima = pontuacaoMaxima;
        this.texto = texto;
    }

    public String descricao() {
        return descricao;
    }

    public int pontuacaoMinima() {
        return pontuacaoMinima;
    }

    public int pontuacaoMaxima() {
        return pontuacaoMaxima;
    }

    public String texto() {
        return texto;
    }

    /** Classifica uma pontuacao de 0 a 100 na faixa correspondente. */
    public static PerfilRisco porPontuacao(int pontuacao) {
        int limitada = Math.max(0, Math.min(100, pontuacao));
        return Arrays.stream(values())
                .filter(p -> limitada >= p.pontuacaoMinima && limitada <= p.pontuacaoMaxima)
                .findFirst()
                .orElse(CONSERVADOR);
    }

    /**
     * Interpreta o perfil recebido como texto (path param), aceitando variacoes de caixa e acento.
     *
     * @throws IllegalArgumentException quando o valor nao corresponde a nenhum perfil
     */
    public static PerfilRisco de(String valor) {
        String normalizado = Risco.Texto.normalizar(valor);
        return Arrays.stream(values())
                .filter(p -> Risco.Texto.normalizar(p.name()).equals(normalizado)
                        || Risco.Texto.normalizar(p.descricao).equals(normalizado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Perfil inválido: '%s'. Valores aceitos: Conservador, Moderado, Agressivo."
                                .formatted(valor)));
    }

    /** Indica se um produto com o risco informado e aderente ao perfil. */
    public boolean aceita(Risco risco) {
        return switch (this) {
            case CONSERVADOR -> risco == Risco.BAIXO;
            case MODERADO -> risco != Risco.ALTO;
            case AGRESSIVO -> true;
        };
    }

    /**
     * Grau de aderencia (0 a 3) usado para ordenar produtos candidatos. Quanto maior, mais
     * alinhado o produto esta ao perfil do cliente.
     */
    public int aderencia(Risco risco) {
        return switch (this) {
            case CONSERVADOR -> switch (risco) {
                case BAIXO -> 3;
                case MEDIO -> 1;
                case ALTO -> 0;
            };
            case MODERADO -> switch (risco) {
                case BAIXO -> 2;
                case MEDIO -> 3;
                case ALTO -> 1;
            };
            case AGRESSIVO -> switch (risco) {
                case BAIXO -> 1;
                case MEDIO -> 2;
                case ALTO -> 3;
            };
        };
    }
}

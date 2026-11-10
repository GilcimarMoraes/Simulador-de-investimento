package br.com.investimentos.dominio;

/** Liquidez do produto: resgate diario ou apenas no vencimento. */
public enum Liquidez {
    DIARIA("Diária"),
    VENCIMENTO("No vencimento");

    private final String descricao;

    Liquidez(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    public static Liquidez de(String valor) {
        String normalizado = Risco.Texto.normalizar(valor);
        return switch (normalizado) {
            case "DIARIA", "D+0", "D+1" -> DIARIA;
            case "VENCIMENTO", "NO VENCIMENTO" -> VENCIMENTO;
            default -> throw new IllegalArgumentException("Liquidez desconhecida: " + valor);
        };
    }
}

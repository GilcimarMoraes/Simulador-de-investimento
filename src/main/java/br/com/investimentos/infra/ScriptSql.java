package br.com.investimentos.infra;

import java.util.ArrayList;
import java.util.List;

/**
 * Leitor simples de scripts SQL: remove comentarios de linha e separa comandos por ';'.
 * Suficiente para o schema e os dados de seed, que nao possuem blocos procedurais.
 */
public final class ScriptSql {

    private ScriptSql() {
    }

    public static List<String> comandos(String script) {
        String semComentarios = script.replaceAll("(?m)--.*$", "");
        List<String> comandos = new ArrayList<>();
        for (String parte : semComentarios.split(";")) {
            String comando = parte.trim();
            if (!comando.isEmpty()) {
                comandos.add(comando);
            }
        }
        return comandos;
    }
}

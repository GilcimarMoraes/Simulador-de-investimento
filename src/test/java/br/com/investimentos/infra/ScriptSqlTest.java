package br.com.investimentos.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptSqlTest {

    @Test
    @DisplayName("Remove comentarios e separa comandos por ponto e virgula")
    void separaComandos() {
        String script = """
                -- comentario inicial
                CREATE TABLE a (id INTEGER); -- comentario de linha

                INSERT INTO a (id) VALUES (1);
                """;

        List<String> comandos = ScriptSql.comandos(script);

        assertEquals(2, comandos.size());
        assertEquals("CREATE TABLE a (id INTEGER)", comandos.get(0));
        assertEquals("INSERT INTO a (id) VALUES (1)", comandos.get(1));
    }

    @Test
    @DisplayName("Script vazio ou apenas comentarios nao gera comandos")
    void scriptVazio() {
        assertEquals(List.of(), ScriptSql.comandos(""));
        assertEquals(List.of(), ScriptSql.comandos("-- nada aqui\n"));
    }
}

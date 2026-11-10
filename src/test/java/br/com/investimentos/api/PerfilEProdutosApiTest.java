package br.com.investimentos.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class PerfilEProdutosApiTest extends BaseApiTest {

    @Test
    @DisplayName("Cliente 123 tem perfil Moderado com 65 pontos e fatores auditaveis")
    void perfilDoClienteDeDemonstracao() {
        JsonPath json = comoDemo().get("/perfil-risco/123")
                .then().statusCode(200).extract().jsonPath();

        assertEquals("Moderado", json.getString("perfil"));
        assertEquals(65, json.getInt("pontuacao"));
        assertEquals("Perfil equilibrado entre segurança e rentabilidade.", json.getString("descricao"));
        assertEquals(4, json.getList("fatores").size());
        assertDecimal("308000.00", decimal(json, "totalInvestido"));
        assertEquals(5, json.getInt("movimentacoes180Dias"));
    }

    @Test
    @DisplayName("Perfis Conservador e Agressivo sao calculados pelos dados de seed")
    void perfisExtremos() {
        assertEquals("Conservador", comoDemo().get("/perfil-risco/1")
                .then().statusCode(200).extract().path("perfil"));
        assertEquals("Agressivo", comoDemo().get("/perfil-risco/2")
                .then().statusCode(200).extract().path("perfil"));
        assertEquals(8, comoDemo().get("/perfil-risco/3")
                .then().statusCode(200).extract().jsonPath().getInt("pontuacao"));
    }

    @Test
    @DisplayName("Cliente inexistente retorna 404 no perfil")
    void perfilClienteInexistente() {
        comoDemo().get("/perfil-risco/999")
                .then().statusCode(404)
                .body("type", endsWith("/nao-encontrado"));
    }

    @Test
    @DisplayName("Conservador recebe apenas produtos de risco baixo")
    void recomendacoesConservador() {
        comoDemo().get("/produtos-recomendados/Conservador")
                .then().statusCode(200)
                .body("size()", is(5))
                .body("risco", everyItem(is("Baixo")));
    }

    @Test
    @DisplayName("Moderado recebe risco baixo e medio, sem alto")
    void recomendacoesModerado() {
        comoDemo().get("/produtos-recomendados/Moderado")
                .then().statusCode(200)
                .body("size()", is(8))
                .body("risco", hasItem("Médio"))
                .body("risco", not(hasItem("Alto")));
    }

    @Test
    @DisplayName("Agressivo recebe todo o catalogo, incluindo alto risco")
    void recomendacoesAgressivo() {
        comoDemo().get("/produtos-recomendados/Agressivo")
                .then().statusCode(200)
                .body("size()", is(10))
                .body("risco", hasItem("Alto"));
    }

    @Test
    @DisplayName("Perfil invalido retorna 400 com os valores aceitos")
    void perfilInvalido() {
        comoDemo().get("/produtos-recomendados/Arrojado")
                .then().statusCode(400)
                .body("detail", containsString("Conservador, Moderado, Agressivo"));
    }

    @Test
    @DisplayName("Historico de investimentos do cliente 123 e ordenado por data")
    void historicoDeInvestimentos() {
        List<Map<String, Object>> investimentos = comoDemo().get("/investimentos/123")
                .then().statusCode(200).extract().jsonPath().getList("$");

        assertEquals(5, investimentos.size());
        JsonPath json = comoDemo().get("/investimentos/123").then().statusCode(200).extract().jsonPath();
        assertDecimal("3000.00", decimal(json, "[0].valor"));

        List<String> datas = json.getList("data");
        List<String> ordenadas = datas.stream().sorted(java.util.Comparator.reverseOrder()).toList();
        assertEquals(ordenadas, datas);
    }

    @Test
    @DisplayName("Catalogo expoe apenas produtos ativos")
    void catalogoDeProdutos() {
        JsonPath json = comoDemo().get("/produtos")
                .then().statusCode(200).extract().jsonPath();

        assertEquals(10, json.getList("$").size());
        assertTrue(json.getList("id").stream().noneMatch(id -> ((Number) id).intValue() == 111));
    }

    @Test
    @DisplayName("Investimentos de cliente inexistente retornam 404")
    void investimentosClienteInexistente() {
        comoDemo().get("/investimentos/999").then().statusCode(404);
    }
}

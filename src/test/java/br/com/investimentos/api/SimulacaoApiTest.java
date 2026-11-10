package br.com.investimentos.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class SimulacaoApiTest extends BaseApiTest {

    @Test
    @DisplayName("POST /simular-investimento devolve o envelope completo do desafio")
    void envelopeDaSimulacao() {
        JsonPath json = simular(123, "10000.00", 12, "CDB");

        assertEquals(101, json.getInt("produtoValidado.id"));
        assertEquals("CDB Caixa 2026", json.getString("produtoValidado.nome"));
        assertEquals("CDB", json.getString("produtoValidado.tipo"));
        assertEquals(0.12, json.getDouble("produtoValidado.rentabilidade"), 1e-9);
        assertEquals("Baixo", json.getString("produtoValidado.risco"));

        assertDecimal("11200.00", decimal(json, "resultadoSimulacao.valorFinal"));
        assertEquals(0.12, json.getDouble("resultadoSimulacao.rentabilidadeEfetiva"), 1e-9);
        assertEquals(12, json.getInt("resultadoSimulacao.prazoMeses"));
        assertEquals("Moderado", json.getString("perfilCliente"));
        assertNotNull(json.getString("dataSimulacao"));
        assertEquals(123, json.getLong("clienteId"));
        assertEquals(0, json.getList("alertas").size());
    }

    @Test
    @DisplayName("A simulacao e persistida e aparece no historico")
    void simulacaoPersistida() {
        long id = simular(123, "10000.00", 12, "CDB").getLong("simulacaoId");

        JsonPath historico = comoDemo().get("/simulacoes")
                .then().statusCode(200).extract().jsonPath();

        assertEquals(1, historico.getList("$").size());
        assertEquals(id, historico.getLong("[0].id"));
        assertEquals(123, historico.getLong("[0].clienteId"));
        assertEquals("CDB Caixa 2026", historico.getString("[0].produto"));
        assertDecimal("10000.00", decimal(historico, "[0].valorInvestido"));
        assertDecimal("11200.00", decimal(historico, "[0].valorFinal"));
        assertEquals(12, historico.getInt("[0].prazoMeses"));
    }

    @Test
    @DisplayName("GET /simulacoes/por-produto-dia agrega quantidade e media do dia")
    void agregadoPorProdutoDia() {
        simular(123, "10000.00", 12, "CDB");
        simular(123, "20000.00", 12, "CDB");

        JsonPath agregado = comoDemo().get("/simulacoes/por-produto-dia")
                .then().statusCode(200).extract().jsonPath();

        assertEquals(1, agregado.getList("$").size());
        assertEquals("CDB Caixa 2026", agregado.getString("[0].produto"));
        assertEquals(2, agregado.getLong("[0].quantidadeSimulacoes"));
        assertDecimal("16800.00", decimal(agregado, "[0].mediaValorFinal"));
    }

    @Test
    @DisplayName("Produto premium e escolhido quando o valor justifica")
    void selecionaProdutoPremium() {
        JsonPath json = simular(123, "60000.00", 12, "CDB");

        assertEquals(110, json.getInt("produtoValidado.id"));
        assertEquals(0.125, json.getDouble("produtoValidado.rentabilidade"), 1e-9);
        assertDecimal("67500.00", decimal(json, "resultadoSimulacao.valorFinal"));
        assertDecimal("66000.00", decimal(json, "resultadoSimulacao.valorFinalLiquido"));
    }

    @Test
    @DisplayName("Filtro por cliente restringe o historico")
    void filtroPorCliente() {
        simular(123, "10000.00", 12, "CDB");
        simular(1, "10000.00", 12, "CDB");

        assertEquals(2, comoDemo().get("/simulacoes").then().statusCode(200)
                .extract().jsonPath().getList("$").size());
        assertEquals(1, comoDemo().queryParam("clienteId", 123).get("/simulacoes")
                .then().statusCode(200).extract().jsonPath().getList("$").size());
    }

    @Test
    @DisplayName("Tipo em minusculas e aceito")
    void tipoToleranteACaixa() {
        assertEquals(101, simular(123, "10000.00", 12, "cdb").getInt("produtoValidado.id"));
    }

    @Test
    @DisplayName("Cliente inexistente retorna 404")
    void clienteInexistente() {
        comoDemo().contentType("application/json")
                .body(corpoSimulacao(999, "10000.00", 12, "CDB"))
                .post("/simular-investimento")
                .then().statusCode(404)
                .body("type", endsWith("/nao-encontrado"))
                .body("detail", containsString("Cliente 999"));
    }

    @Test
    @DisplayName("Sem produto que atenda valor/prazo retorna 422")
    void semProdutoCompativel() {
        comoDemo().contentType("application/json")
                .body(corpoSimulacao(123, "10000.00", 72, "CDB"))
                .post("/simular-investimento")
                .then().statusCode(422)
                .body("type", endsWith("/regra-de-negocio"))
                .body("detail", containsString("Nenhum produto do tipo CDB"));
    }

    @Test
    @DisplayName("Valor abaixo do minimo de todos os produtos retorna 422")
    void valorAbaixoDoMinimo() {
        comoDemo().contentType("application/json")
                .body(corpoSimulacao(123, "100.00", 12, "CDB"))
                .post("/simular-investimento")
                .then().statusCode(422);
    }

    @Test
    @DisplayName("Tipo desconhecido retorna 400 com os tipos aceitos")
    void tipoDesconhecido() {
        comoDemo().contentType("application/json")
                .body(corpoSimulacao(123, "10000.00", 12, "XYZ"))
                .post("/simular-investimento")
                .then().statusCode(400)
                .body("detail", containsString("Tipos disponíveis"));
    }

    @Test
    @DisplayName("Validacao de campos retorna 400 com a lista de erros")
    void validacaoDeCampos() {
        comoDemo().contentType("application/json")
                .body("{}")
                .post("/simular-investimento")
                .then().statusCode(400)
                .body("type", endsWith("/validacao"))
                .body("erros.size()", org.hamcrest.Matchers.equalTo(4));
    }
}

package br.com.investimentos.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class TratamentoErrosTest extends BaseApiTest {

    @Test
    @DisplayName("JSON malformado retorna 400 corpo-invalido")
    void jsonMalformado() {
        comoDemo().contentType("application/json")
                .body("{\"clienteId\":")
                .post("/simular-investimento")
                .then().statusCode(400)
                .body("type", endsWith("/corpo-invalido"));
    }

    @Test
    @DisplayName("Rota inexistente retorna 404 com Problem Details")
    void rotaInexistente() {
        comoDemo().get("/rota-que-nao-existe")
                .then().statusCode(404)
                .body("type", endsWith("/requisicao"))
                .body("title", is("Recurso não encontrado"));
    }

    @Test
    @DisplayName("Metodo HTTP nao suportado retorna 405")
    void metodoNaoPermitido() {
        comoDemo().delete("/simulacoes")
                .then().statusCode(405)
                .body("title", is("Método não permitido"));
    }

    @Test
    @DisplayName("Content-Type nao suportado retorna 415")
    void contentTypeNaoSuportado() {
        comoDemo().contentType("text/plain")
                .body("clienteId=123")
                .post("/simular-investimento")
                .then().statusCode(415)
                .body("title", is("Tipo de mídia não suportado"));
    }

    @Test
    @DisplayName("Toda resposta de erro carrega requestId correlacionavel no header e no corpo")
    void correlationId() {
        Response resposta = comoDemo().get("/perfil-risco/999").then().statusCode(404).extract().response();

        String header = resposta.getHeader("X-Request-Id");
        String corpo = resposta.jsonPath().getString("requestId");
        assertEquals(header, corpo);
        org.junit.jupiter.api.Assertions.assertNotNull(header);
        org.junit.jupiter.api.Assertions.assertNotNull(resposta.jsonPath().getString("timestamp"));
    }

    @Test
    @DisplayName("Content-Type ausente em POST com corpo JSON retorna 415")
    void semContentType() {
        given().auth().oauth2(tokenDemo)
                .body(corpoSimulacao(123, "10000.00", 12, "CDB"))
                .post("/simular-investimento")
                .then().statusCode(415);
    }
}

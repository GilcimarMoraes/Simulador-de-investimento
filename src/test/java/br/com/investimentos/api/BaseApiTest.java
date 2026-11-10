package br.com.investimentos.api;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Base dos testes de API: limpa o banco e obtem tokens reais via /auth/token. */
public abstract class BaseApiTest {

    @Inject
    LimpezaBanco limpeza;

    protected String tokenDemo;
    protected String tokenAdmin;

    @BeforeEach
    void prepararCenario() {
        limpeza.executar();
        tokenDemo = autenticar("demo", "demo123");
        tokenAdmin = autenticar("admin", "admin123");
    }

    protected String autenticar(String usuario, String senha) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", usuario, "password", senha))
                .when().post("/auth/token")
                .then().statusCode(200)
                .extract().path("accessToken");
    }

    protected RequestSpecification comoDemo() {
        return given().auth().oauth2(tokenDemo);
    }

    protected RequestSpecification comoAdmin() {
        return given().auth().oauth2(tokenAdmin);
    }

    protected JsonPath simular(long clienteId, String valor, int prazo, String tipo) {
        return comoDemo()
                .contentType(ContentType.JSON)
                .body(corpoSimulacao(clienteId, valor, prazo, tipo))
                .when().post("/simular-investimento")
                .then().statusCode(200)
                .extract().jsonPath();
    }

    protected String corpoSimulacao(long clienteId, String valor, int prazo, String tipo) {
        return """
                {"clienteId": %d, "valor": %s, "prazoMeses": %d, "tipoProduto": "%s"}
                """.formatted(clienteId, valor, prazo, tipo);
    }

    protected static BigDecimal decimal(JsonPath json, String caminho) {
        Object valor = json.get(caminho);
        assertNotNull(valor, () -> "campo ausente na resposta: " + caminho);
        return new BigDecimal(valor.toString());
    }

    protected static void assertDecimal(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual),
                () -> "esperado " + esperado + " mas foi " + atual);
    }
}

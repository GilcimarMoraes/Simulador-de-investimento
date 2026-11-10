package br.com.investimentos.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class AutenticacaoETelemetriaTest extends BaseApiTest {

    @Test
    @DisplayName("Login valido devolve token, papel e cliente")
    void loginValido() {
        JsonPath json = given().contentType("application/json")
                .body(Map.of("username", "demo", "password", "demo123"))
                .post("/auth/token")
                .then().statusCode(200)
                .body("tokenType", is("Bearer"))
                .body("usuario", is("demo"))
                .body("roles", hasItem("USER"))
                .extract().jsonPath();

        assertNotNull(json.getString("accessToken"));
        assertEquals(3600, json.getInt("expiresIn"));
        assertEquals(123, json.getInt("clienteId"));
        assertNotNull(json.getString("expiraEm"));
    }

    @Test
    @DisplayName("GET /auth/me reflete o token atual")
    void usuarioAtual() {
        comoDemo().get("/auth/me")
                .then().statusCode(200)
                .body("usuario", is("demo"))
                .body("clienteId", is(123))
                .body("roles", hasItem("USER"));
    }

    @Test
    @DisplayName("Credenciais invalidas retornam 401 com Problem Details")
    void credenciaisInvalidas() {
        given().contentType("application/json")
                .body(Map.of("username", "demo", "password", "errada"))
                .post("/auth/token")
                .then().statusCode(401)
                .body("type", endsWith("/credenciais-invalidas"))
                .body("detail", containsString("inválidos"));
    }

    @Test
    @DisplayName("Requisicao sem token retorna 401 com desafio Bearer")
    void semToken() {
        given().get("/simulacoes")
                .then().statusCode(401)
                .header("WWW-Authenticate", containsString("Bearer"));
    }

    @Test
    @DisplayName("Token adulterado retorna 401")
    void tokenAdulterado() {
        given().auth().oauth2(tokenDemo + "x").get("/simulacoes").then().statusCode(401);
    }

    @Test
    @DisplayName("Telemetria exige papel ADMIN")
    void telemetriaRestrita() {
        comoDemo().get("/telemetria")
                .then().statusCode(403)
                .body("type", endsWith("/acesso-negado"));
    }

    @Test
    @DisplayName("Telemetria agrega volume por servico apos as chamadas")
    void telemetriaAgregada() {
        comoDemo().get("/perfil-risco/123").then().statusCode(200);
        comoDemo().get("/perfil-risco/1").then().statusCode(200);

        JsonPath json = comoAdmin().get("/telemetria")
                .then().statusCode(200)
                .body("servicos.nome", hasItem("perfil-risco"))
                .extract().jsonPath();

        assertNotNull(json.getString("periodo.inicio"));
        assertNotNull(json.getString("periodo.fim"));

        int posicao = json.getList("servicos.nome").indexOf("perfil-risco");
        assertEquals(2, json.getInt("servicos[" + posicao + "].quantidadeChamadas"));
        assertThatPositivo(json.getLong("servicos[" + posicao + "].mediaTempoRespostaMs"));
        assertThatPositivo(json.getLong("servicos[" + posicao + "].p95TempoRespostaMs"));
    }

    @Test
    @DisplayName("Intervalo invalido na telemetria retorna 400")
    void intervaloInvalido() {
        comoAdmin().queryParam("inicio", "2026-05-01").queryParam("fim", "2026-01-01")
                .get("/telemetria")
                .then().statusCode(400)
                .body("detail", containsString("inicio deve ser anterior"));
    }

    private static void assertThatPositivo(long valor) {
        org.junit.jupiter.api.Assertions.assertTrue(valor >= 0,
                () -> "tempo de resposta deveria ser nao negativo: " + valor);
    }

    @Test
    @DisplayName("Data malformada na telemetria retorna 400")
    void dataMalformada() {
        comoAdmin().queryParam("inicio", "01/05/2026").get("/telemetria")
                .then().statusCode(400)
                .body("detail", containsString("AAAA-MM-DD"));
    }
}

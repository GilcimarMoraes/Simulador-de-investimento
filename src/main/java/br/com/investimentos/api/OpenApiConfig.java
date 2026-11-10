package br.com.investimentos.api;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

/** Metadados do contrato OpenAPI publicado em /q/openapi e /q/swagger-ui. */
@OpenAPIDefinition(
        info = @Info(
                title = "Painel de Investimentos API",
                version = "1.0.0",
                description = """
                        API de simulação de investimentos com perfil de risco dinâmico.
                        Autentique em POST /auth/token com as credenciais de demonstração
                        (demo/demo123 ou admin/admin123) e use o token no botão Authorize.
                        """,
                contact = @Contact(name = "Time de Investimentos")),
        servers = @Server(url = "http://localhost:8080", description = "Ambiente local"),
        tags = {})
public class OpenApiConfig {
}

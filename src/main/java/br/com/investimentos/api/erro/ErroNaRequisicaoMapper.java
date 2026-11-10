package br.com.investimentos.api.erro;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/** Preserva o status de erros HTTP gerados pelo proprio JAX-RS (405, 406, 415, rota inexistente). */
@Provider
public class ErroNaRequisicaoMapper implements ExceptionMapper<WebApplicationException> {

    @Inject
    ErroFactory fabrica;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException excecao) {
        int status = excecao.getResponse() != null
                ? excecao.getResponse().getStatus()
                : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

        if (status == 400 && causaJsonInvalido(excecao)) {
            ErroResponse corpo = fabrica.criar(400, "corpo-invalido", "Corpo da requisição inválido",
                    "O corpo enviado não é um JSON válido ou não corresponde ao contrato esperado.",
                    List.of(), uriInfo.getPath());
            return Response.status(400).type(MediaType.APPLICATION_JSON).entity(corpo).build();
        }

        String titulo = switch (status) {
            case 404 -> "Recurso não encontrado";
            case 405 -> "Método não permitido";
            case 406 -> "Formato de resposta não suportado";
            case 415 -> "Tipo de mídia não suportado";
            default -> "Requisição inválida";
        };
        String detalhe = switch (status) {
            case 404 -> "A rota ou o recurso solicitado não existe.";
            case 405 -> "O método HTTP utilizado não é suportado por esta rota.";
            case 415 -> "O Content-Type enviado não é suportado; utilize application/json.";
            default -> "A requisição não pôde ser processada.";
        };
        ErroResponse corpo = fabrica.criar(status, "requisicao", titulo, detalhe, List.of(),
                uriInfo.getPath());
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }

    private boolean causaJsonInvalido(Throwable lancada) {
        Throwable atual = lancada;
        while (atual != null) {
            if (atual instanceof JsonProcessingException) {
                return true;
            }
            atual = atual.getCause() == atual ? null : atual.getCause();
        }
        return false;
    }
}

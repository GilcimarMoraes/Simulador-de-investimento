package br.com.investimentos.api;

import br.com.investimentos.dominio.RegistroRequisicao;
import br.com.investimentos.servico.TelemetriaService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

/**
 * Filtro unico de observabilidade: correlaciona a requisicao (X-Request-Id), adiciona headers de
 * seguranca e registra a amostra de telemetria dos endpoints anotados com {@link Telemetrado}.
 *
 * <p>A telemetria nunca interrompe a resposta: falhas de gravacao sao apenas logadas.</p>
 */
@Provider
@Priority(Priorities.USER)
public class FiltroObservabilidade implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String PROPRIEDADE_INICIO = "investimentos.inicioNanos";
    private static final String HEADER_CORRELACAO = "X-Request-Id";

    @Context
    ResourceInfo resourceInfo;

    @Context
    SecurityContext securityContext;

    @Inject
    RequestContexto contexto;

    @Inject
    TelemetriaService telemetria;

    @Override
    public void filter(ContainerRequestContext requisicao) {
        String id = requisicao.getHeaderString(HEADER_CORRELACAO);
        if (id == null || id.isBlank() || id.length() > 80) {
            id = UUID.randomUUID().toString();
        }
        contexto.id(id);
        contexto.recebidaEm(Instant.now());
        MDC.put("requestId", id);
        requisicao.setProperty(PROPRIEDADE_INICIO, System.nanoTime());
    }

    @Override
    public void filter(ContainerRequestContext requisicao, ContainerResponseContext resposta) {
        resposta.getHeaders().add(HEADER_CORRELACAO, contexto.id());
        resposta.getHeaders().add("X-Content-Type-Options", "nosniff");
        resposta.getHeaders().add("X-Frame-Options", "DENY");
        resposta.getHeaders().add("Referrer-Policy", "no-referrer");

        try {
            Telemetrado anotacao = anotacaoDoEndpoint();
            if (anotacao != null) {
                Object inicio = requisicao.getProperty(PROPRIEDADE_INICIO);
                long duracaoMs = inicio instanceof Long nanos
                        ? Math.max(0, (System.nanoTime() - nanos) / 1_000_000)
                        : 0;
                String usuario = securityContext != null && securityContext.getUserPrincipal() != null
                        ? securityContext.getUserPrincipal().getName()
                        : "anonimo";
                telemetria.registrar(new RegistroRequisicao(
                        anotacao.value(),
                        requisicao.getMethod(),
                        requisicao.getUriInfo().getPath(),
                        resposta.getStatus(),
                        duracaoMs,
                        usuario,
                        Instant.now()));
            }
        } catch (RuntimeException e) {
            // Telemetria e observabilidade nao podem comprometer a resposta funcional.
            org.jboss.logging.Logger.getLogger(FiltroObservabilidade.class)
                    .warnf(e, "Falha ao registrar telemetria da requisicao %s", contexto.id());
        } finally {
            MDC.remove("requestId");
        }
    }

    private Telemetrado anotacaoDoEndpoint() {
        if (resourceInfo == null || resourceInfo.getResourceMethod() == null) {
            return null;
        }
        Method metodo = resourceInfo.getResourceMethod();
        Telemetrado anotacao = metodo.getAnnotation(Telemetrado.class);
        if (anotacao == null && resourceInfo.getResourceClass() != null) {
            anotacao = resourceInfo.getResourceClass().getAnnotation(Telemetrado.class);
        }
        return anotacao;
    }
}

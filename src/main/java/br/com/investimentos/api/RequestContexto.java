package br.com.investimentos.api;

import jakarta.enterprise.context.RequestScoped;

import java.time.Instant;
import java.util.UUID;

/** Dados da requisicao corrente usados em logs, telemetria e respostas de erro. */
@RequestScoped
public class RequestContexto {

    private String id = UUID.randomUUID().toString();
    private Instant recebidaEm = Instant.now();

    public String id() {
        return id;
    }

    public void id(String id) {
        this.id = id;
    }

    public Instant recebidaEm() {
        return recebidaEm;
    }

    public void recebidaEm(Instant recebidaEm) {
        this.recebidaEm = recebidaEm;
    }
}

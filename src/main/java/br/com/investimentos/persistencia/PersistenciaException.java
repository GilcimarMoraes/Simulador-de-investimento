package br.com.investimentos.persistencia;

/** Falha de infraestrutura de banco de dados, traduzida para HTTP 500 pela camada de API. */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

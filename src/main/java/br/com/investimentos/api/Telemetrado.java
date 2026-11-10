package br.com.investimentos.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um endpoint que deve ter volume e tempo de resposta registrados.
 * O valor e o nome logico do servico exposto em GET /telemetria.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Telemetrado {
    String value();
}

package br.com.investimentos.api.dto;

import java.time.LocalDate;

public record PeriodoDTO(LocalDate inicio, LocalDate fim) {
}

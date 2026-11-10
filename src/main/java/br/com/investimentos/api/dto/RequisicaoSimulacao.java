package br.com.investimentos.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

/** Envelope de entrada do POST /simular-investimento. */
@Schema(description = "Solicitação de simulação de investimento")
public record RequisicaoSimulacao(
        @Schema(description = "Identificador do cliente", example = "123")
        @NotNull(message = "clienteId é obrigatório")
        @Positive(message = "clienteId deve ser positivo")
        Long clienteId,

        @Schema(description = "Valor a investir, em reais", example = "10000.00")
        @NotNull(message = "valor é obrigatório")
        @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
        @Digits(integer = 15, fraction = 2, message = "valor deve ter no máximo 2 casas decimais")
        BigDecimal valor,

        @Schema(description = "Prazo da aplicação em meses", example = "12")
        @NotNull(message = "prazoMeses é obrigatório")
        @Min(value = 1, message = "prazoMeses deve ser no mínimo 1")
        @Max(value = 1200, message = "prazoMeses deve ser no máximo 1200")
        Integer prazoMeses,

        @Schema(description = "Tipo do produto (CDB, LCI, LCA, TESOURO, FUNDO)", example = "CDB")
        @NotBlank(message = "tipoProduto é obrigatório")
        @Size(max = 40, message = "tipoProduto deve ter no máximo 40 caracteres")
        String tipoProduto) {
}

package br.unisinos.uni4read.dto;

import br.unisinos.uni4read.entity.ReadingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AtualizarAtividadeRequestDTO(
        @NotNull(message = "Informe o status") ReadingStatus status,
        @Min(value = 1, message = "A nota deve ser de 1 a 5")
        @Max(value = 5, message = "A nota deve ser de 1 a 5") Integer rating) {
}
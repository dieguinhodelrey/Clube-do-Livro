package br.unisinos.uni4read.dto;

import java.util.UUID;

import br.unisinos.uni4read.entity.ReadingStatus;
import jakarta.validation.constraints.NotNull;

public record CriarAtividadeRequestDTO(
        @NotNull(message = "Informe o livro") UUID bookId,
        @NotNull(message = "Informe o status") ReadingStatus status) {
}
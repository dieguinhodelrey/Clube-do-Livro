package br.unisinos.uni4read.dto;

import br.unisinos.uni4read.entity.ReactionType;
import jakarta.validation.constraints.NotNull;

public record ReacaoRequestDTO(@NotNull(message = "Informe a reação") ReactionType type) {
}
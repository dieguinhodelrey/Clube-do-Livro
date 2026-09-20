package br.unisinos.uni4read.dto;

import java.util.UUID;

import br.unisinos.uni4read.entity.ReactionType;

public record ReacaoRequestDTO(UUID userId, ReactionType type) {
}
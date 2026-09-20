package br.unisinos.uni4read.dto;

import java.util.UUID;

import br.unisinos.uni4read.entity.ReactionType;

public record ReacaoDTO(UUID activityId, long likes, long dislikes, ReactionType userReaction) {
}
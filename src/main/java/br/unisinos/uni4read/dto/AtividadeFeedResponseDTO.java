package br.unisinos.uni4read.dto;

import java.time.Instant;
import java.util.UUID;

import br.unisinos.uni4read.entity.ReadingStatus;
import br.unisinos.uni4read.entity.ReactionType;

public record AtividadeFeedResponseDTO(
        UUID id,
        UserResponseDTO user,
        BookResponseDTO book,
        ReadingStatus status,
        Instant createdAt,
        long likes,
        long dislikes,
        ReactionType currentUserReaction
) {
}
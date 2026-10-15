package br.unisinos.uni4read.dto;

import java.util.UUID;

import br.unisinos.uni4read.entity.ReadingStatus;

public record AtualizarAtividadeRequestDTO(UUID userId, ReadingStatus status, Integer rating) {
}
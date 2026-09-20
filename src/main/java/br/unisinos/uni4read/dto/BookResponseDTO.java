package br.unisinos.uni4read.dto;

import java.util.UUID;

public record BookResponseDTO(UUID id, String title, String author, String coverUrl) {
}
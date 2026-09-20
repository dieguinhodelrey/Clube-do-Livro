package br.unisinos.uni4read.dto;

import java.util.UUID;

public record UserResponseDTO(UUID id, String name, String avatarUrl) {
}
package br.unisinos.uni4read.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarLivroRequestDTO(
        @NotBlank(message = "Informe o título") @Size(max = 255, message = "Título deve ter até 255 caracteres") String title,
        @NotBlank(message = "Informe o autor") @Size(max = 255, message = "Autor deve ter até 255 caracteres") String author) {
}
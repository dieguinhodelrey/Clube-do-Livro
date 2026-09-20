package br.unisinos.uni4read.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.unisinos.uni4read.dto.AtividadeFeedResponseDTO;
import br.unisinos.uni4read.dto.CriarAtividadeRequestDTO;
import br.unisinos.uni4read.dto.ReacaoDTO;
import br.unisinos.uni4read.dto.ReacaoRequestDTO;
import br.unisinos.uni4read.service.AtividadeService;
import br.unisinos.uni4read.service.ReacaoService;

@RestController
@RequestMapping("/api/feed")
@CrossOrigin(origins = "*")
public class FeedController {

    private final AtividadeService atividadeService;
    private final ReacaoService reacaoService;

    public FeedController(AtividadeService atividadeService, ReacaoService reacaoService) {
        this.atividadeService = atividadeService;
        this.reacaoService = reacaoService;
    }

    @GetMapping
    public List<AtividadeFeedResponseDTO> listarFeed(@RequestParam(required = false) UUID userId) {
        return atividadeService.listarFeed(userId);
    }

    @PostMapping
    public ResponseEntity<AtividadeFeedResponseDTO> criarAtividade(
            @RequestBody CriarAtividadeRequestDTO request) {
        AtividadeFeedResponseDTO response = atividadeService.registrarAtividade(request);
        return ResponseEntity.created(URI.create("/api/feed/" + response.id())).body(response);
    }

    @PostMapping("/{id}/reacao")
    public ReacaoDTO alternarReacao(
            @PathVariable UUID id,
            @RequestBody ReacaoRequestDTO request) {
        return reacaoService.alternar(id, request);
    }
}
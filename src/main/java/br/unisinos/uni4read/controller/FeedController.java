package br.unisinos.uni4read.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import br.unisinos.uni4read.entity.User;

import br.unisinos.uni4read.dto.AtividadeFeedResponseDTO;
import br.unisinos.uni4read.dto.AtualizarAtividadeRequestDTO;
import br.unisinos.uni4read.dto.CriarAtividadeRequestDTO;
import br.unisinos.uni4read.dto.ReacaoDTO;
import br.unisinos.uni4read.dto.ReacaoRequestDTO;
import br.unisinos.uni4read.service.AtividadeService;
import br.unisinos.uni4read.service.ReacaoService;
import br.unisinos.uni4read.entity.ReadingStatus;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final AtividadeService atividadeService;
    private final ReacaoService reacaoService;

    public FeedController(AtividadeService atividadeService, ReacaoService reacaoService) {
        this.atividadeService = atividadeService;
        this.reacaoService = reacaoService;
    }

    @GetMapping
    public List<AtividadeFeedResponseDTO> listarFeed(
            Authentication authentication,
            @RequestParam(required = false) ReadingStatus status) {
        return atividadeService.listarFeed(currentUser(authentication), status);
    }

    @PatchMapping("/{id}")
    public AtividadeFeedResponseDTO atualizar(
            @PathVariable UUID id,
            @RequestBody AtualizarAtividadeRequestDTO request,
            Authentication authentication) {
        return atividadeService.atualizar(id, request, currentUser(authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable UUID id,
            Authentication authentication) {
        atividadeService.excluir(id, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<AtividadeFeedResponseDTO> criarAtividade(
            @RequestBody CriarAtividadeRequestDTO request,
            Authentication authentication) {
        AtividadeFeedResponseDTO response = atividadeService.registrarAtividade(request, currentUser(authentication));
        return ResponseEntity.created(URI.create("/api/feed/" + response.id())).body(response);
    }

    @PostMapping("/{id}/reacao")
    public ReacaoDTO alternarReacao(
            @PathVariable UUID id,
            @RequestBody ReacaoRequestDTO request,
            Authentication authentication) {
        return reacaoService.alternar(id, request, currentUser(authentication));
    }

    private UUID currentUser(Authentication authentication) {
        return ((User) authentication.getPrincipal()).getId();
    }
}
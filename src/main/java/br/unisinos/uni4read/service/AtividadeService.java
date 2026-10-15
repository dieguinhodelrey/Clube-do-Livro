package br.unisinos.uni4read.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.unisinos.uni4read.dto.AtividadeFeedResponseDTO;
import br.unisinos.uni4read.dto.AtualizarAtividadeRequestDTO;
import br.unisinos.uni4read.dto.BookResponseDTO;
import br.unisinos.uni4read.dto.CriarAtividadeRequestDTO;
import br.unisinos.uni4read.dto.UserResponseDTO;
import br.unisinos.uni4read.entity.FeedActivity;
import br.unisinos.uni4read.entity.ReactionType;
import br.unisinos.uni4read.entity.ReadingStatus;
import br.unisinos.uni4read.repository.BookRepository;
import br.unisinos.uni4read.repository.FeedActivityRepository;
import br.unisinos.uni4read.repository.ReactionRepository;
import br.unisinos.uni4read.repository.UserRepository;

@Service
public class AtividadeService {

    private final FeedActivityRepository feedActivityRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public AtividadeService(
            FeedActivityRepository feedActivityRepository,
            ReactionRepository reactionRepository,
            UserRepository userRepository,
            BookRepository bookRepository) {
        this.feedActivityRepository = feedActivityRepository;
        this.reactionRepository = reactionRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public List<AtividadeFeedResponseDTO> listarFeed(UUID userId) {
        return feedActivityRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(activity -> toResponse(activity, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AtividadeFeedResponseDTO> listarFeed(UUID userId, ReadingStatus status) {
        List<FeedActivity> activities = status == null
                ? feedActivityRepository.findAllByOrderByCreatedAtDesc()
                : feedActivityRepository.findAllByStatusOrderByCreatedAtDesc(status);
        return activities.stream().map(activity -> toResponse(activity, userId)).toList();
    }

    @Transactional
    public AtividadeFeedResponseDTO registrarAtividade(CriarAtividadeRequestDTO request) {
        FeedActivity activity = new FeedActivity();
        activity.setUser(userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado")));
        activity.setBook(bookRepository.findById(request.bookId())
                .orElseThrow(() -> new IllegalArgumentException("Livro nao encontrado")));
        activity.setStatus(request.status());
        activity.setRating(null);
        FeedActivity savedActivity = feedActivityRepository.save(activity);
        return toResponse(savedActivity, request.userId());
    }

    @Transactional
    public AtividadeFeedResponseDTO atualizar(UUID activityId, AtualizarAtividadeRequestDTO request) {
        FeedActivity activity = feedActivityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Atividade nao encontrada"));
        if (!activity.getUser().getId().equals(request.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario sem permissao para alterar esta atividade");
        }
        if (request.status() == null) {
            throw new IllegalArgumentException("Status obrigatorio");
        }
        if (activity.getStatus() == ReadingStatus.READ && request.status() != ReadingStatus.READ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Uma atividade lida nao pode voltar para o status lendo");
        }
        if (request.rating() != null && (request.rating() < 1 || request.rating() > 5)) {
            throw new IllegalArgumentException("A avaliacao deve estar entre 1 e 5");
        }
        activity.setStatus(request.status());
        activity.setRating(request.status() == ReadingStatus.READ ? request.rating() : null);
        return toResponse(activity, request.userId());
    }

    @Transactional
    public void excluir(UUID activityId, UUID userId) {
        FeedActivity activity = feedActivityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Atividade nao encontrada"));
        if (!activity.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario sem permissao para excluir esta atividade");
        }
        reactionRepository.deleteAll(reactionRepository.findAll().stream()
                .filter(reaction -> reaction.getActivity().getId().equals(activityId))
                .toList());
        feedActivityRepository.delete(activity);
    }

    private AtividadeFeedResponseDTO toResponse(FeedActivity activity, UUID userId) {
        return new AtividadeFeedResponseDTO(
                activity.getId(),
                new UserResponseDTO(activity.getUser().getId(), activity.getUser().getName()),
                new BookResponseDTO(activity.getBook().getId(), activity.getBook().getTitle(), activity.getBook().getAuthor()),
                activity.getStatus(),
                activity.getRating(),
                activity.getCreatedAt(),
                reactionRepository.countByActivityIdAndType(activity.getId(), ReactionType.LIKE),
                reactionRepository.countByActivityIdAndType(activity.getId(), ReactionType.DISLIKE),
                userId == null ? null : reactionRepository.findByActivityIdAndUserId(activity.getId(), userId)
                        .map(reaction -> reaction.getType()).orElse(null));
    }
}

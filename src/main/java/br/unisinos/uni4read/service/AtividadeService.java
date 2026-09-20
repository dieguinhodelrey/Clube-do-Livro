package br.unisinos.uni4read.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.unisinos.uni4read.dto.AtividadeFeedResponseDTO;
import br.unisinos.uni4read.dto.BookResponseDTO;
import br.unisinos.uni4read.dto.CriarAtividadeRequestDTO;
import br.unisinos.uni4read.dto.UserResponseDTO;
import br.unisinos.uni4read.entity.FeedActivity;
import br.unisinos.uni4read.entity.ReactionType;
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

    @Transactional
    public AtividadeFeedResponseDTO registrarAtividade(CriarAtividadeRequestDTO request) {
        FeedActivity activity = new FeedActivity();
        activity.setUser(userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado")));
        activity.setBook(bookRepository.findById(request.bookId())
                .orElseThrow(() -> new IllegalArgumentException("Livro nao encontrado")));
        activity.setStatus(request.status());
        FeedActivity savedActivity = feedActivityRepository.save(activity);
        return toResponse(savedActivity, request.userId());
    }

    private AtividadeFeedResponseDTO toResponse(FeedActivity activity, UUID userId) {
        return new AtividadeFeedResponseDTO(
                activity.getId(),
                new UserResponseDTO(activity.getUser().getId(), activity.getUser().getName(), activity.getUser().getAvatarUrl()),
                new BookResponseDTO(activity.getBook().getId(), activity.getBook().getTitle(), activity.getBook().getAuthor(), activity.getBook().getCoverUrl()),
                activity.getStatus(),
                activity.getCreatedAt(),
                reactionRepository.countByActivityIdAndType(activity.getId(), ReactionType.LIKE),
                reactionRepository.countByActivityIdAndType(activity.getId(), ReactionType.DISLIKE),
                userId == null ? null : reactionRepository.findByActivityIdAndUserId(activity.getId(), userId)
                        .map(reaction -> reaction.getType()).orElse(null));
    }
}

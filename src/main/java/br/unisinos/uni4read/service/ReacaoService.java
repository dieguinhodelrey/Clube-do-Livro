package br.unisinos.uni4read.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.unisinos.uni4read.dto.ReacaoDTO;
import br.unisinos.uni4read.dto.ReacaoRequestDTO;
import br.unisinos.uni4read.entity.FeedActivity;
import br.unisinos.uni4read.entity.Reaction;
import br.unisinos.uni4read.entity.ReactionType;
import br.unisinos.uni4read.repository.FeedActivityRepository;
import br.unisinos.uni4read.repository.ReactionRepository;
import br.unisinos.uni4read.repository.UserRepository;

@Service
public class ReacaoService {

    private final ReactionRepository reactionRepository;
    private final FeedActivityRepository feedActivityRepository;
    private final UserRepository userRepository;

    public ReacaoService(
            ReactionRepository reactionRepository,
            FeedActivityRepository feedActivityRepository,
            UserRepository userRepository) {
        this.reactionRepository = reactionRepository;
        this.feedActivityRepository = feedActivityRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReacaoDTO alternar(UUID activityId, ReacaoRequestDTO request) {
        FeedActivity activity = feedActivityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Atividade nao encontrada"));
        userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        Reaction reaction = reactionRepository.findByActivityIdAndUserId(activityId, request.userId()).orElse(null);
        if (reaction != null && reaction.getType() == request.type()) {
            reactionRepository.delete(reaction);
        } else if (reaction != null) {
            reaction.setType(request.type());
            reactionRepository.save(reaction);
        } else {
            Reaction newReaction = new Reaction();
            newReaction.setActivity(activity);
            newReaction.setUser(userRepository.getReferenceById(request.userId()));
            newReaction.setType(request.type());
            reactionRepository.save(newReaction);
        }
        reactionRepository.flush();
        return toResponse(activityId, request.userId());
    }

    private ReacaoDTO toResponse(UUID activityId, UUID userId) {
        return new ReacaoDTO(
                activityId,
                reactionRepository.countByActivityIdAndType(activityId, ReactionType.LIKE),
                reactionRepository.countByActivityIdAndType(activityId, ReactionType.DISLIKE),
                reactionRepository.findByActivityIdAndUserId(activityId, userId)
                        .map(Reaction::getType).orElse(null));
    }
}
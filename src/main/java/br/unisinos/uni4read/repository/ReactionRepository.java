package br.unisinos.uni4read.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.unisinos.uni4read.entity.Reaction;
import br.unisinos.uni4read.entity.ReactionType;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

	long countByActivityIdAndType(UUID activityId, ReactionType type);

	Optional<Reaction> findByActivityIdAndUserId(UUID activityId, UUID userId);
}
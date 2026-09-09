package br.unisinos.uni4read.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import br.unisinos.uni4read.entity.FeedActivity;
import br.unisinos.uni4read.entity.Reaction;
import br.unisinos.uni4read.entity.ReactionType;
import br.unisinos.uni4read.entity.ReadingStatus;
import br.unisinos.uni4read.entity.User;
import jakarta.persistence.EntityManager;

@DataJpaTest
class FeedActivityRepositoryTest {

    private static final UUID FIRST_ACTIVITY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Autowired
    private FeedActivityRepository feedActivityRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void loadsSeededActivitiesAndReactions() {
        FeedActivity activity = feedActivityRepository.findById(FIRST_ACTIVITY_ID).orElseThrow();
        Reaction reaction = reactionRepository.findAll().get(0);

        assertThat(feedActivityRepository.count()).isEqualTo(2);
        assertThat(activity.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(activity.getUser().getName()).isEqualTo("Alice Johnson");
        assertThat(activity.getBook().getTitle()).isEqualTo("The Pragmatic Programmer");
        assertThat(reaction.getType()).isIn(ReactionType.LIKE, ReactionType.DISLIKE);
        assertThat(reaction.getActivity()).isNotNull();
        assertThat(reaction.getUser()).isNotNull();
    }

    @Test
    void assignsCreationTimeWhenSavingAnActivity() {
        FeedActivity activity = new FeedActivity();
        activity.setUser(userRepository.findById(UUID.fromString("10000000-0000-0000-0000-000000000001")).orElseThrow());
        activity.setBook(feedActivityRepository.findById(FIRST_ACTIVITY_ID).orElseThrow().getBook());
        activity.setStatus(ReadingStatus.READING);

        FeedActivity savedActivity = feedActivityRepository.saveAndFlush(activity);

        assertThat(savedActivity.getId()).isNotNull();
        assertThat(savedActivity.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateReactionForTheSameUserAndActivity() {
        FeedActivity activity = feedActivityRepository.findById(FIRST_ACTIVITY_ID).orElseThrow();
        User user = userRepository.findById(UUID.fromString("10000000-0000-0000-0000-000000000002")).orElseThrow();
        Reaction duplicateReaction = new Reaction();
        duplicateReaction.setActivity(activity);
        duplicateReaction.setUser(user);
        duplicateReaction.setType(ReactionType.DISLIKE);

        entityManager.persist(duplicateReaction);

        assertThatThrownBy(entityManager::flush)
            .isInstanceOf(ConstraintViolationException.class);
    }
}
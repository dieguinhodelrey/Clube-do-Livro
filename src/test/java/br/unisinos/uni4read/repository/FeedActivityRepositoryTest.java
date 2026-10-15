package br.unisinos.uni4read.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import br.unisinos.uni4read.entity.FeedActivity;
import br.unisinos.uni4read.entity.Book;
import br.unisinos.uni4read.entity.Reaction;
import br.unisinos.uni4read.entity.ReactionType;
import br.unisinos.uni4read.entity.ReadingStatus;
import br.unisinos.uni4read.entity.User;
import jakarta.persistence.EntityManager;

@DataJpaTest
class FeedActivityRepositoryTest {

    private FeedActivity firstActivity;

    @Autowired
    private FeedActivityRepository feedActivityRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void createFixtures() {
        User alice = userRepository.findById(UUID.fromString("10000000-0000-0000-0000-000000000001")).orElseThrow();
        User bruno = userRepository.findById(UUID.fromString("10000000-0000-0000-0000-000000000002")).orElseThrow();

        Book firstBook = new Book();
        firstBook.setTitle("Test Book One");
        firstBook.setAuthor("Test Author One");
        bookRepository.save(firstBook);

        Book secondBook = new Book();
        secondBook.setTitle("Test Book Two");
        secondBook.setAuthor("Test Author Two");
        bookRepository.save(secondBook);

        firstActivity = new FeedActivity();
        firstActivity.setUser(alice);
        firstActivity.setBook(firstBook);
        firstActivity.setStatus(ReadingStatus.READING);
        entityManager.persist(firstActivity);

        FeedActivity secondActivity = new FeedActivity();
        secondActivity.setUser(bruno);
        secondActivity.setBook(secondBook);
        secondActivity.setStatus(ReadingStatus.READ);
        entityManager.persist(secondActivity);
        entityManager.flush();
    }

    @Test
    void loadsActivitiesAndRelationships() {
        FeedActivity activity = feedActivityRepository.findById(firstActivity.getId()).orElseThrow();

        assertThat(feedActivityRepository.count()).isEqualTo(2);
        assertThat(activity.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(activity.getUser().getName()).isEqualTo("Alice Johnson");
        assertThat(activity.getBook().getTitle()).isEqualTo("Test Book One");
    }

    @Test
    void assignsCreationTimeWhenSavingAnActivity() {
        FeedActivity activity = new FeedActivity();
        activity.setUser(userRepository.findById(UUID.fromString("10000000-0000-0000-0000-000000000001")).orElseThrow());
        activity.setBook(firstActivity.getBook());
        activity.setStatus(ReadingStatus.READING);

        FeedActivity savedActivity = feedActivityRepository.saveAndFlush(activity);

        assertThat(savedActivity.getId()).isNotNull();
        assertThat(savedActivity.getCreatedAt()).isNotNull();
    }

    @Test
    void filtersActivitiesByReadingStatus() {
        assertThat(feedActivityRepository.findAllByStatusOrderByCreatedAtDesc(ReadingStatus.READING))
            .extracting(FeedActivity::getStatus)
            .containsExactly(ReadingStatus.READING);
    }

    @Test
    void persistsOptionalRating() {
        FeedActivity activity = feedActivityRepository.findById(firstActivity.getId()).orElseThrow();
        activity.setRating(5);

        FeedActivity savedActivity = feedActivityRepository.saveAndFlush(activity);

        assertThat(savedActivity.getRating()).isEqualTo(5);
    }

    @Test
    void rejectsDuplicateReactionForTheSameUserAndActivity() {
        FeedActivity activity = feedActivityRepository.findById(firstActivity.getId()).orElseThrow();
        User user = userRepository.findById(UUID.fromString("10000000-0000-0000-0000-000000000002")).orElseThrow();
        Reaction existingReaction = new Reaction();
        existingReaction.setActivity(activity);
        existingReaction.setUser(user);
        existingReaction.setType(ReactionType.LIKE);
        reactionRepository.saveAndFlush(existingReaction);

        Reaction duplicateReaction = new Reaction();
        duplicateReaction.setActivity(activity);
        duplicateReaction.setUser(user);
        duplicateReaction.setType(ReactionType.DISLIKE);

        entityManager.persist(duplicateReaction);

        assertThatThrownBy(entityManager::flush)
            .isInstanceOf(ConstraintViolationException.class);
    }
}
package br.unisinos.uni4read.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.unisinos.uni4read.entity.Book;
import br.unisinos.uni4read.entity.FeedActivity;
import br.unisinos.uni4read.entity.ReadingStatus;
import br.unisinos.uni4read.entity.User;
import br.unisinos.uni4read.repository.BookRepository;
import br.unisinos.uni4read.repository.FeedActivityRepository;
import br.unisinos.uni4read.repository.ReactionRepository;
import br.unisinos.uni4read.repository.UserRepository;
import br.unisinos.uni4read.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReactionIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private BookRepository books;
    @Autowired
    private FeedActivityRepository activities;
    @Autowired
    private ReactionRepository reactions;
    @Autowired
    private UserRepository users;
    @Autowired
    private JwtService jwt;

    private FeedActivity reading;
    private User bruno;
    private String aliceToken;
    private String brunoToken;

    @BeforeEach
    void createReading() {
        User alice = users.findByEmailIgnoreCase("alice@example.com").orElseThrow();
        bruno = users.findByEmailIgnoreCase("bruno@example.com").orElseThrow();
        aliceToken = "Bearer " + jwt.createAccessToken(alice);
        brunoToken = "Bearer " + jwt.createAccessToken(bruno);
        Book book = new Book();
        book.setTitle("Livro de reações");
        book.setAuthor("Autora de teste");
        books.save(book);
        reading = new FeedActivity();
        reading.setBook(book);
        reading.setUser(alice);
        reading.setStatus(ReadingStatus.READING);
        activities.saveAndFlush(reading);
    }

    @Test
    void reactionCanBeAddedChangedAndRemoved() throws Exception {
        react(reading.getId(), brunoToken, "LIKE")
                .andExpect(jsonPath("$.likes").value(1))
                .andExpect(jsonPath("$.dislikes").value(0))
                .andExpect(jsonPath("$.userReaction").value("LIKE"));
        react(reading.getId(), brunoToken, "DISLIKE")
                .andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.dislikes").value(1))
                .andExpect(jsonPath("$.userReaction").value("DISLIKE"));
        react(reading.getId(), brunoToken, "DISLIKE")
                .andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.dislikes").value(0))
                .andExpect(jsonPath("$.userReaction").isEmpty());
        assertThat(reactions.count()).isZero();
    }

    @Test
    void reactionsAndSelectionAreIndependentForEachReader() throws Exception {
        react(reading.getId(), aliceToken, "LIKE");
        react(reading.getId(), brunoToken, "LIKE").andExpect(jsonPath("$.likes").value(2));
        react(reading.getId(), aliceToken, "LIKE").andExpect(jsonPath("$.likes").value(1));
        mvc.perform(get("/api/feed").header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentUserReaction").isEmpty())
                .andExpect(jsonPath("$[0].likes").value(1));
        mvc.perform(get("/api/feed").header("Authorization", brunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentUserReaction").value("LIKE"))
                .andExpect(jsonPath("$[0].likes").value(1));
        assertThat(reactions.count()).isEqualTo(1);
    }

    @Test
    void deletingAReadingRemovesOnlyItsReactionsAndPreservesTheSharedBook() throws Exception {
        FeedActivity other = new FeedActivity();
        other.setBook(reading.getBook());
        other.setUser(bruno);
        other.setStatus(ReadingStatus.READING);
        activities.saveAndFlush(other);
        react(reading.getId(), aliceToken, "LIKE");
        react(reading.getId(), brunoToken, "DISLIKE");
        react(other.getId(), brunoToken, "LIKE");
        mvc.perform(delete("/api/feed/{id}", reading.getId()).header("Authorization", aliceToken))
                .andExpect(status().isNoContent());
        assertThat(activities.existsById(reading.getId())).isFalse();
        assertThat(activities.existsById(other.getId())).isTrue();
        assertThat(books.existsById(reading.getBook().getId())).isTrue();
        assertThat(reactions.count()).isEqualTo(1);
        assertThat(reactions.findByActivityIdAndUserId(other.getId(), bruno.getId())).isPresent();
    }

    private ResultActions react(UUID activityId, String token, String type) throws Exception {
        return mvc.perform(post("/api/feed/{id}/reacao", activityId).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"" + type + "\"}"))
                .andExpect(status().isOk());
    }
}

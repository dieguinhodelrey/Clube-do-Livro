package br.unisinos.uni4read.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.unisinos.uni4read.entity.Book;
import br.unisinos.uni4read.entity.FeedActivity;
import br.unisinos.uni4read.entity.ReadingStatus;
import br.unisinos.uni4read.entity.User;
import br.unisinos.uni4read.repository.BookRepository;
import br.unisinos.uni4read.repository.FeedActivityRepository;
import br.unisinos.uni4read.repository.UserRepository;
import br.unisinos.uni4read.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReadingStatusIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private BookRepository books;
    @Autowired
    private FeedActivityRepository activities;
    @Autowired
    private UserRepository users;
    @Autowired
    private JwtService jwt;

    private FeedActivity reading;
    private String aliceToken;
    private String brunoToken;

    @BeforeEach
    void createReading() {
        User alice = users.findByEmailIgnoreCase("alice@example.com").orElseThrow();
        aliceToken = "Bearer " + jwt.createAccessToken(alice);
        brunoToken = "Bearer " + jwt.createAccessToken(
                users.findByEmailIgnoreCase("bruno@example.com").orElseThrow());
        Book book = new Book();
        book.setTitle("Livro para concluir");
        book.setAuthor("Autora de teste");
        books.save(book);
        reading = new FeedActivity();
        reading.setBook(book);
        reading.setUser(alice);
        reading.setStatus(ReadingStatus.READING);
        activities.saveAndFlush(reading);
    }

    @Test
    void completionWithoutRatingIsAllowedAndCannotBeReversed() throws Exception {
        mvc.perform(patch("/api/feed/{id}", reading.getId()).header("Authorization", aliceToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"))
                .andExpect(jsonPath("$.rating").isEmpty());
        mvc.perform(patch("/api/feed/{id}", reading.getId()).header("Authorization", aliceToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READING\"}"))
                .andExpect(status().isConflict());
        assertEquals(ReadingStatus.READ, activities.findById(reading.getId()).orElseThrow().getStatus());
    }

    @Test
    void ratingCanBeChangedAtBothLimitsAndRemoved() throws Exception {
        for (int rating : new int[] { 1, 5 }) {
            mvc.perform(patch("/api/feed/{id}", reading.getId()).header("Authorization", aliceToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"READ\",\"rating\":" + rating + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rating").value(rating));
        }
        mvc.perform(patch("/api/feed/{id}", reading.getId()).header("Authorization", aliceToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READ\",\"rating\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").isEmpty());
        assertNull(activities.findById(reading.getId()).orElseThrow().getRating());
    }

    @Test
    void invalidRatingsAndRatingsWhileReadingDoNotChangeTheActivity() throws Exception {
        for (String body : new String[] {
                "{\"status\":\"READ\",\"rating\":0}",
                "{\"status\":\"READ\",\"rating\":6}",
                "{\"status\":\"READ\",\"rating\":1.5}",
                "{\"status\":\"READING\",\"rating\":3}" }) {
            mvc.perform(patch("/api/feed/{id}", reading.getId()).header("Authorization", aliceToken)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(ReadingStatus.READING, activities.findById(reading.getId()).orElseThrow().getStatus());
        assertNull(activities.findById(reading.getId()).orElseThrow().getRating());
    }

    @Test
    void anotherReaderCannotCompleteOrRateTheActivity() throws Exception {
        mvc.perform(patch("/api/feed/{id}", reading.getId()).header("Authorization", brunoToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READ\",\"rating\":5}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/feed/{id}", reading.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READ\"}"))
                .andExpect(status().isUnauthorized());
        assertEquals(ReadingStatus.READING, activities.findById(reading.getId()).orElseThrow().getStatus());
    }
}

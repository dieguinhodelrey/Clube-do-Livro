package br.unisinos.uni4read.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class FeedListsIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private BookRepository books;
    @Autowired
    private FeedActivityRepository activities;
    @Autowired
    private JwtService jwt;

    private User alice;
    private User bruno;
    private FeedActivity aliceReading;
    private FeedActivity aliceRead;
    private FeedActivity brunoReading;

    @BeforeEach
    void createReadings() {
        alice = users.findByEmailIgnoreCase("alice@example.com").orElseThrow();
        bruno = users.findByEmailIgnoreCase("bruno@example.com").orElseThrow();
        Book book = new Book();
        book.setTitle("Shared test book");
        book.setAuthor("Test author");
        books.save(book);
        aliceReading = reading(alice, book, ReadingStatus.READING);
        aliceRead = reading(alice, book, ReadingStatus.READ);
        brunoReading = reading(bruno, book, ReadingStatus.READING);
        activities.flush();
    }

    private FeedActivity reading(User user, Book book, ReadingStatus status) {
        FeedActivity activity = new FeedActivity();
        activity.setUser(user);
        activity.setBook(book);
        activity.setStatus(status);
        return activities.save(activity);
    }

    private String authorization(User user) {
        return "Bearer " + jwt.createAccessToken(user);
    }

    @Test
    void personalListsFilterByOwnerAndStatus() throws Exception {
        mvc.perform(get("/api/feed/mine").param("status", "READING")
                .header("Authorization", authorization(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(aliceReading.getId().toString()));
        mvc.perform(get("/api/feed/mine").param("status", "READ")
                .header("Authorization", authorization(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(aliceRead.getId().toString()));
        mvc.perform(get("/api/feed/mine").param("status", "READING")
                .header("Authorization", authorization(bruno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(brunoReading.getId().toString()));
    }

    @Test
    void personalListWithoutStatusStillUsesAuthenticatedOwner() throws Exception {
        mvc.perform(get("/api/feed/mine").param("userId", bruno.getId().toString())
                .header("Authorization", authorization(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        aliceReading.getId().toString(), aliceRead.getId().toString())));
    }

    @Test
    void feedRemainsCollectiveIncludingItsExistingStatusFilter() throws Exception {
        mvc.perform(get("/api/feed").header("Authorization", authorization(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/feed").param("status", "READING")
                .header("Authorization", authorization(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        aliceReading.getId().toString(), brunoReading.getId().toString())));
    }

    @Test
    void personalListsRequireAuthenticationAndRejectInvalidStatus() throws Exception {
        mvc.perform(get("/api/feed/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/feed/mine").param("status", "INVALID")
                .header("Authorization", authorization(alice)))
                .andExpect(status().isBadRequest());
    }
}

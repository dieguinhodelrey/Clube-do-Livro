package br.unisinos.uni4read.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class ReadingDeletionIntegrationTest {

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
    private Book book;
    private FeedActivity ownReading;
    private FeedActivity otherReading;

    @BeforeEach
    void createReadings() {
        alice = users.findByEmailIgnoreCase("alice@example.com").orElseThrow();
        User bruno = users.findByEmailIgnoreCase("bruno@example.com").orElseThrow();
        book = new Book();
        book.setTitle("Shared deletion test");
        book.setAuthor("Test author");
        books.save(book);
        ownReading = reading(alice);
        otherReading = reading(bruno);
        activities.flush();
    }

    private FeedActivity reading(User user) {
        FeedActivity activity = new FeedActivity();
        activity.setUser(user);
        activity.setBook(book);
        activity.setStatus(ReadingStatus.READING);
        return activities.save(activity);
    }

    @Test
    void deletionPreservesTheBookAndOtherAccountsReadings() throws Exception {
        mvc.perform(delete("/api/feed/{id}", ownReading.getId())
                .header("Authorization", "Bearer " + jwt.createAccessToken(alice)))
                .andExpect(status().isNoContent());
        assertThat(activities.existsById(ownReading.getId())).isFalse();
        assertThat(activities.existsById(otherReading.getId())).isTrue();
        assertThat(books.existsById(book.getId())).isTrue();
    }

    @Test
    void anotherAccountCannotDeleteTheReading() throws Exception {
        mvc.perform(delete("/api/feed/{id}", otherReading.getId())
                .header("Authorization", "Bearer " + jwt.createAccessToken(alice)))
                .andExpect(status().isForbidden());
        assertThat(activities.existsById(otherReading.getId())).isTrue();
    }

    @Test
    void deletionRequiresAuthentication() throws Exception {
        mvc.perform(delete("/api/feed/{id}", ownReading.getId()))
                .andExpect(status().isUnauthorized());
        assertThat(activities.existsById(ownReading.getId())).isTrue();
    }
}

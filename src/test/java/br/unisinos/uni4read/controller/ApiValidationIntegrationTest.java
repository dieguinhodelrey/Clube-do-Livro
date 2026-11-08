package br.unisinos.uni4read.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.transaction.annotation.Transactional;

import br.unisinos.uni4read.entity.Book;
import br.unisinos.uni4read.entity.FeedActivity;
import br.unisinos.uni4read.entity.ReadingStatus;
import br.unisinos.uni4read.repository.BookRepository;
import br.unisinos.uni4read.repository.FeedActivityRepository;
import br.unisinos.uni4read.repository.UserRepository;
import br.unisinos.uni4read.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiValidationIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private BookRepository books;
    @Autowired private FeedActivityRepository activities;
    @Autowired private UserRepository users;
    @Autowired private JwtService jwt;
    private String authorization;
    private FeedActivity reading;

    @BeforeEach
    void createReading() {
        var alice = users.findByEmailIgnoreCase("alice@example.com").orElseThrow();
        authorization = "Bearer " + jwt.createAccessToken(alice);
        Book book = new Book();
        book.setTitle("Validation test");
        book.setAuthor("Test author");
        books.save(book);
        reading = new FeedActivity();
        reading.setUser(alice);
        reading.setBook(book);
        reading.setStatus(ReadingStatus.READING);
        activities.saveAndFlush(reading);
    }

    @Test
    void invalidBooksAreRejectedWithoutSaving() throws Exception {
        long before = books.count();
        for (String body : new String[] {"{}", "{\"title\":\"  \",\"author\":\"A\"}",
                "{\"title\":\"A\",\"author\":\"  \"}", "{\"title\":\"" + "x".repeat(256) + "\",\"author\":\"A\"}"}) {
            mvc.perform(post("/api/books").header("Authorization", authorization)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fields").isMap());
        }
        assertThat(books.count()).isEqualTo(before);
    }

    @Test
    void missingReadingDataAndReactionTypeAreRejected() throws Exception {
        long before = activities.count();
        mvc.perform(post("/api/feed").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.bookId").exists())
                .andExpect(jsonPath("$.fields.status").exists());
        mvc.perform(post("/api/feed/{id}/reacao", reading.getId()).header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.type").exists());
        assertThat(activities.count()).isEqualTo(before);
    }

    @Test
    void invalidRatingsAndFractionalValuesAreRejected() throws Exception {
        for (String rating : new String[] {"0", "6", "1.5"}) {
            mvc.perform(patch("/api/feed/{id}", reading.getId()).header("Authorization", authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"READ\",\"rating\":" + rating + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
        mvc.perform(patch("/api/feed/{id}", reading.getId()).header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READING\",\"rating\":3}"))
                .andExpect(status().isBadRequest());
        assertThat(reading.getStatus()).isEqualTo(ReadingStatus.READING);
    }

    @Test
    void malformedJsonEnumsAndIdentifiersUseJsonErrors() throws Exception {
        mvc.perform(post("/api/books").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/feed/mine").header("Authorization", authorization).param("status", "BAD"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.status").exists());
        mvc.perform(patch("/api/feed/not-a-uuid").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READ\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.id").exists());
    }

    @Test
    void missingReferencesReturnNotFound() throws Exception {
        mvc.perform(post("/api/feed").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":\"" + UUID.randomUUID() + "\",\"status\":\"READING\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(patch("/api/feed/{id}", UUID.randomUUID()).header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READ\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void authenticationErrorsUseJsonAndMeRequiresASession() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.email").exists());
    }
}

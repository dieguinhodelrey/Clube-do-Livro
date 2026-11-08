package br.unisinos.uni4read.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import br.unisinos.uni4read.repository.BookRepository;
import br.unisinos.uni4read.repository.UserRepository;
import br.unisinos.uni4read.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookRegistrationIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private BookRepository books;
    @Autowired
    private UserRepository users;
    @Autowired
    private JwtService jwt;

    private String aliceToken;
    private String brunoToken;

    @BeforeEach
    void authenticateReaders() {
        aliceToken = "Bearer " + jwt.createAccessToken(
                users.findByEmailIgnoreCase("alice@example.com").orElseThrow());
        brunoToken = "Bearer " + jwt.createAccessToken(
                users.findByEmailIgnoreCase("bruno@example.com").orElseThrow());
    }

    @Test
    void emptyCatalogIsPublicAndReturnsAnEmptyArray() throws Exception {
        mvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void populatedCatalogIsSharedAndContainsIdTitleAndAuthor() throws Exception {
        Book original = book("Livro do catálogo", "Autora de teste");
        for (String token : new String[] { aliceToken, brunoToken }) {
            mvc.perform(get("/api/books").header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(original.getId().toString()))
                    .andExpect(jsonPath("$[0].title").value("Livro do catálogo"))
                    .andExpect(jsonPath("$[0].author").value("Autora de teste"));
        }
        mvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(original.getId().toString()));
    }

    @Test
    void newBookTrimsTitleAndAuthor() throws Exception {
        mvc.perform(post("/api/books").header("Authorization", aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"  Livro de teste  ","author":"  Autora de teste  "}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Livro de teste"))
                .andExpect(jsonPath("$.author").value("Autora de teste"));
    }

    @Test
    void reusePreservesSharedMetadataAndDoesNotCreateAnotherBook() throws Exception {
        Book original = book("Livro de teste", "Autora de teste");
        long count = books.count();
        mvc.perform(post("/api/books").header("Authorization", brunoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"  LIVRO DE TESTE  ","author":"  autora de teste  "}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(original.getId().toString()))
                .andExpect(jsonPath("$.title").value(original.getTitle()))
                .andExpect(jsonPath("$.author").value(original.getAuthor()));
        books.flush();
        assertEquals(count, books.count());
        assertEquals("Livro de teste", books.findById(original.getId()).orElseThrow().getTitle());
    }

    @Test
    void sameTitleWithAnotherAuthorCreatesADistinctBook() throws Exception {
        book("Livro de teste", "Autora de teste");
        long count = books.count();
        mvc.perform(post("/api/books").header("Authorization", aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Livro de teste","author":"Outro autor"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("Outro autor"));
        assertEquals(count + 1, books.count());
    }

    @Test
    void reusedBookCanBeLinkedToTheAuthenticatedReadersActivity() throws Exception {
        Book original = book("Livro de teste", "Autora de teste");
        mvc.perform(post("/api/books").header("Authorization", brunoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"livro de teste","author":"autora de teste"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(original.getId().toString()));
        mvc.perform(post("/api/feed").header("Authorization", brunoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"bookId":"%s","status":"READING"}
                        """.formatted(original.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.book.id").value(original.getId().toString()))
                .andExpect(jsonPath("$.user.email").value("bruno@example.com"))
                .andExpect(jsonPath("$.status").value("READING"));
    }

    private Book book(String title, String author) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        return books.saveAndFlush(book);
    }
}

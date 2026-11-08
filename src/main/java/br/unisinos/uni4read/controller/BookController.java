package br.unisinos.uni4read.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.unisinos.uni4read.dto.BookResponseDTO;
import br.unisinos.uni4read.dto.CriarLivroRequestDTO;
import br.unisinos.uni4read.entity.Book;
import br.unisinos.uni4read.repository.BookRepository;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping
    public List<BookResponseDTO> listar() {
        return bookRepository.findAll().stream()
            .map(book -> new BookResponseDTO(book.getId(), book.getTitle(), book.getAuthor()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<BookResponseDTO> criar(@Valid @RequestBody CriarLivroRequestDTO request) {
        String title = request.title().trim();
        String author = request.author().trim();
        Book book = bookRepository.findByTitleIgnoreCaseAndAuthorIgnoreCase(title, author)
                .orElseGet(() -> {
                    Book created = new Book();
                    created.setTitle(title);
                    created.setAuthor(author);
                    return bookRepository.save(created);
                });
        BookResponseDTO response = new BookResponseDTO(book.getId(), book.getTitle(), book.getAuthor());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
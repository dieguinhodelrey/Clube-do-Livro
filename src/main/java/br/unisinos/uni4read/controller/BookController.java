package br.unisinos.uni4read.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.unisinos.uni4read.dto.BookResponseDTO;
import br.unisinos.uni4read.dto.CriarLivroRequestDTO;
import br.unisinos.uni4read.entity.Book;
import br.unisinos.uni4read.repository.BookRepository;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "*")
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
    public ResponseEntity<BookResponseDTO> criar(@RequestBody CriarLivroRequestDTO request) {
        Book book = bookRepository.findByTitleIgnoreCaseAndAuthorIgnoreCase(request.title(), request.author())
                .orElseGet(Book::new);
        book.setTitle(request.title());
        book.setAuthor(request.author());
        Book saved = bookRepository.save(book);
        BookResponseDTO response = new BookResponseDTO(saved.getId(), saved.getTitle(), saved.getAuthor());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
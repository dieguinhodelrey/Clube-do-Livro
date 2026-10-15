package br.unisinos.uni4read.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.unisinos.uni4read.entity.Book;

public interface BookRepository extends JpaRepository<Book, UUID> {

	Optional<Book> findByTitleIgnoreCaseAndAuthorIgnoreCase(String title, String author);
}
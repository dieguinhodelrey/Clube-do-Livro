package br.unisinos.uni4read.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.unisinos.uni4read.entity.Book;

public interface BookRepository extends JpaRepository<Book, UUID> {
}
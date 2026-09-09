package br.unisinos.uni4read.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.unisinos.uni4read.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
}
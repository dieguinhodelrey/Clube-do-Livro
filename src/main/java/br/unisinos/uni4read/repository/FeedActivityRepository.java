package br.unisinos.uni4read.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.unisinos.uni4read.entity.FeedActivity;

public interface FeedActivityRepository extends JpaRepository<FeedActivity, UUID> {
}
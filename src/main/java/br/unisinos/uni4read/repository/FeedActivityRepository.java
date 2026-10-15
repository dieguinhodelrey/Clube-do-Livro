package br.unisinos.uni4read.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.unisinos.uni4read.entity.FeedActivity;
import br.unisinos.uni4read.entity.ReadingStatus;

public interface FeedActivityRepository extends JpaRepository<FeedActivity, UUID> {

	List<FeedActivity> findAllByOrderByCreatedAtDesc();

	List<FeedActivity> findAllByStatusOrderByCreatedAtDesc(ReadingStatus status);
}
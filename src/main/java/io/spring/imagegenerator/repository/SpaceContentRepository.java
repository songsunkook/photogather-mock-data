package io.spring.imagegenerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.spring.imagegenerator.entity.SpaceContent;

@Repository
public interface SpaceContentRepository extends JpaRepository<SpaceContent, Long> {
}

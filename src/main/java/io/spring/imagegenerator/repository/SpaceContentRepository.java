package io.spring.imagegenerator.repository;

import io.spring.imagegenerator.entity.SpaceContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpaceContentRepository extends JpaRepository<SpaceContent, Long> {
}
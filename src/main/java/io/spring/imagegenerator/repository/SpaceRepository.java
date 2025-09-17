package io.spring.imagegenerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.spring.imagegenerator.entity.Space;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {
}

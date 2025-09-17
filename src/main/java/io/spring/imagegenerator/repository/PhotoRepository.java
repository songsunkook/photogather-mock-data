package io.spring.imagegenerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.spring.imagegenerator.entity.Photo;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
}

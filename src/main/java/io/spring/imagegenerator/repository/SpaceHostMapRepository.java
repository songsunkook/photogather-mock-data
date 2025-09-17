package io.spring.imagegenerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.spring.imagegenerator.entity.SpaceHostMap;

@Repository
public interface SpaceHostMapRepository extends JpaRepository<SpaceHostMap, Long> {
}

package io.spring.imagegenerator.repository;

import io.spring.imagegenerator.entity.SpaceHostMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpaceHostMapRepository extends JpaRepository<SpaceHostMap, Long> {
}
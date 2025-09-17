package io.spring.imagegenerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.spring.imagegenerator.entity.Host;

@Repository
public interface HostRepository extends JpaRepository<Host, Long> {
}

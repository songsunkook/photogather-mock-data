package io.spring.imagegenerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.spring.imagegenerator.entity.Guest;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {
}

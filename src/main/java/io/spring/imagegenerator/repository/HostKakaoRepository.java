package io.spring.imagegenerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.spring.imagegenerator.entity.HostKakao;

@Repository
public interface HostKakaoRepository extends JpaRepository<HostKakao, Long> {
}

package io.spring.imagegenerator.repository;

import io.spring.imagegenerator.entity.HostKakao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HostKakaoRepository extends JpaRepository<HostKakao, Long> {
}
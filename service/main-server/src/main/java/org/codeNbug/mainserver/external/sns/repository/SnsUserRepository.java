package org.codeNbug.mainserver.external.sns.repository;

import org.codeNbug.mainserver.external.sns.Entity.SnsUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SnsUserRepository extends JpaRepository<SnsUser, Long> {
    // 소셜 로그인 ID로 사용자 찾기
    Optional<SnsUser> findBySocialId(String socialId);
}
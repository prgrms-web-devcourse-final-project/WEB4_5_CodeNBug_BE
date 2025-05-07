package org.codenbug.user.sns.repository;

import java.util.Optional;

import org.codenbug.user.sns.Entity.SnsUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnsUserRepository extends JpaRepository<SnsUser, Long> {
    // 소셜 로그인 ID로 사용자 찾기
    Optional<SnsUser> findBySocialId(String socialId);
}
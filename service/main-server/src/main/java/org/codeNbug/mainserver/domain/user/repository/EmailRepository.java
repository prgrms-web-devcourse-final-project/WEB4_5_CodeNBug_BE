package org.codeNbug.mainserver.domain.user.repository;

import java.util.Optional;

import org.codeNbug.mainserver.domain.user.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {
    // 인증코드 발송한 이메일 주소 조회
    public Optional<Email> findByEmail(String email);
}
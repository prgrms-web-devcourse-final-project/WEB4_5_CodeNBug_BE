package org.codeNbug.mainserver.domain.user.repository;

import org.codeNbug.mainserver.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {
    /**
     * 이메일로 사용자 조회
     *
     * @param email 조회할 사용자의 이메일
     * @return Optional<User> 조회된 사용자 (없으면 빈 Optional)
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     *
     * @param email 확인할 이메일
     * @return boolean 이메일이 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.location WHERE u.email = :email")
    Optional<User> findByEmailWithAddresses(@Param("email") String email);
}

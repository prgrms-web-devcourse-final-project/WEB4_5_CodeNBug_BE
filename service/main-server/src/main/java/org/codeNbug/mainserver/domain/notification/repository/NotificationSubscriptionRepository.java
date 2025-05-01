package org.codeNbug.mainserver.domain.notification.repository;

import org.codeNbug.mainserver.domain.notification.entity.NotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 알림 구독 정보에 접근하기 위한 리포지토리 인터페이스
 */
@Repository
public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {

    /**
     * 사용자 ID로 구독 정보 찾기
     *
     * @param userId 사용자 ID
     * @return 사용자의 구독 정보 (없을 경우 빈 Optional)
     */
    Optional<NotificationSubscription> findByUserId(Long userId);

    /**
     * 사용자와 세션으로 구독 정보 찾기
     *
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     * @return 구독 정보 (없을 경우 빈 Optional)
     */
    Optional<NotificationSubscription> findByUserIdAndSessionId(Long userId, String sessionId);

    /**
     * 사용자 ID로 구독 정보 삭제
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
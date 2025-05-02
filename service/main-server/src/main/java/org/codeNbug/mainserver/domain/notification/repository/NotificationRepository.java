package org.codeNbug.mainserver.domain.notification.repository;

import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 알림 엔티티에 대한 데이터 액세스 인터페이스
 * 사용자별 알림 조회 및 알림 상태 관리 기능 제공
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 특정 사용자의 알림 목록을 페이징하여 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return 페이징된 알림 목록
     */
    Page<Notification> findByUserIdOrderBySentAtDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 모든 알림 목록을 최신순으로 조회
     *
     * @param userId 사용자 ID
     * @return 알림 목록
     */
    List<Notification> findByUserIdOrderBySentAtDesc(Long userId);

    /**
     * 특정 사용자의 읽지 않은 알림 수 조회
     *
     * @param userId 사용자 ID
     * @return 읽지 않은 알림 수
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 특정 사용자의 알림 중 특정 타입의 알림만 조회
     *
     * @param userId 사용자 ID
     * @param type 알림 타입
     * @param pageable 페이징 정보
     * @return 페이징된 알림 목록
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.type = :type ORDER BY n.sentAt DESC")
    Page<Notification> findByUserIdAndType(@Param("userId") Long userId, @Param("type") String type, Pageable pageable);

    /**
     * 특정 사용자의 최근 미읽은 알림 조회
     *
     * @param userId 사용자 ID
     * @return 최근 미읽은 알림 목록
     */
    List<Notification> findTop5ByUserIdAndIsReadFalseOrderBySentAtDesc(Long userId);
}
package org.codeNbug.mainserver.domain.user.service;

import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 관련 스케줄링 작업을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSchedulerService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserEmailService userEmailService;

    /**
     * 만료된 비밀번호를 확인하고 알림을 보냅니다.
     */
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시에 실행
    @Transactional
    public void checkExpiringPasswords() {
        log.info(">> 비밀번호 만료 확인 작업 시작");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningDate = now.plusDays(7); // 7일 후 만료 예정인 비밀번호 확인
        
        List<User> expiringUsers = userRepository.findByPasswordExpiredAtBetween(now, warningDate);
        
        for (User user : expiringUsers) {
            log.info(">> 비밀번호 만료 예정 알림: userId={}, email={}, 만료일={}", 
                    user.getUserId(), user.getEmail(), user.getPasswordExpiredAt());
            userEmailService.sendPasswordExpirationWarning(user, user.getPasswordExpiredAt());
        }
        
        log.info(">> 비밀번호 만료 확인 작업 완료");
    }
} 
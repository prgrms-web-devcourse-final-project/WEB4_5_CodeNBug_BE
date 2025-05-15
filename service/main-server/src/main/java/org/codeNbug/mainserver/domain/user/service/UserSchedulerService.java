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
     * 계정 잠금을 자동으로 해제합니다.
     * 잠금 시간이 지난 계정들의 잠금을 해제합니다.
     */
    @Scheduled(fixedRate = 30000) // 3분마다 실행
    @Transactional
    public void autoUnlockAccounts() {
        log.info(">> 계정 잠금 자동 해제 작업 시작");
        
        List<User> lockedUsers = userRepository.findByAccountLockedTrue();
        LocalDateTime now = LocalDateTime.now();
        
        for (User user : lockedUsers) {
            if (user.getLastLoginAt() != null && 
                user.getLastLoginAt().plusMinutes(user.getAccountLockDurationMinutes()).isBefore(now)) {
                log.info(">> 계정 잠금 해제: userId={}, email={}", user.getUserId(), user.getEmail());
                userService.unlockAccount(user);
            }
        }
        
        log.info(">> 계정 잠금 자동 해제 작업 완료");
    }

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
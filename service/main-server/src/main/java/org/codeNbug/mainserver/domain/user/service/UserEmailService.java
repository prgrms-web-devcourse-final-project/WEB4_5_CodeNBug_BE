package org.codeNbug.mainserver.domain.user.service;

import org.codenbug.user.domain.user.entity.User;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 사용자 관련 이메일 발송을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private static final String FROM_EMAIL = "티켓온(Ticket-On)";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    /**
     * 계정 만료 예정 알림 이메일을 발송합니다.
     *
     * @param user 대상 사용자
     * @param expirationDate 만료 예정일
     */
    public void sendAccountExpirationWarning(User user, LocalDateTime expirationDate) {
        try {
            Context context = new Context();
            context.setVariable("userName", user.getName());
            context.setVariable("expirationDate", expirationDate.format(DATE_FORMATTER));
            
            String emailContent = templateEngine.process("account-expiration-warning", context);
            
            sendEmail(
                user.getEmail(),
                "[티켓온(Ticket-On)] 계정 만료 예정 안내",
                emailContent
            );
            
            log.info(">> 계정 만료 예정 알림 이메일 발송 완료: email={}", user.getEmail());
        } catch (Exception e) {
            log.error(">> 계정 만료 예정 알림 이메일 발송 실패: email={}, error={}", 
                    user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * 비밀번호 만료 예정 알림 이메일을 발송합니다.
     *
     * @param user 대상 사용자
     * @param expirationDate 만료 예정일
     */
    public void sendPasswordExpirationWarning(User user, LocalDateTime expirationDate) {
        try {
            Context context = new Context();
            context.setVariable("userName", user.getName());
            context.setVariable("expirationDate", expirationDate.format(DATE_FORMATTER));
            
            String emailContent = templateEngine.process("password-expiration-warning", context);
            
            sendEmail(
                user.getEmail(),
                "[티켓온(Ticket-On)] 비밀번호 만료 예정 안내",
                emailContent
            );
            
            log.info(">> 비밀번호 만료 예정 알림 이메일 발송 완료: email={}", user.getEmail());
        } catch (Exception e) {
            log.error(">> 비밀번호 만료 예정 알림 이메일 발송 실패: email={}, error={}", 
                    user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * 이메일을 발송합니다.
     *
     * @param to 수신자 이메일
     * @param subject 이메일 제목
     * @param content 이메일 내용 (HTML)
     */
    private void sendEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(FROM_EMAIL);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        
        mailSender.send(message);
    }
} 
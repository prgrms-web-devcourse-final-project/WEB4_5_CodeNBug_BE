package org.codeNbug.mainserver.domain.user.service;

import java.security.SecureRandom;

import org.codenbug.user.redis.repository.RedisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.properties.auth-code-expiration-millis}")
    private Long authCodeExpirationMillis; // 인증코드 만료 시간 (밀리초 단위)

    private final JavaMailSender javaMailSender;
    private final RedisRepository redisRepository;
    private static final String senderEmail = "jounghyeon123@gmail.com"; // 발신자 이메일

    private String createCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        
        // 6자리 숫자 생성
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10)); // 0-9 사이의 숫자
        }
        
        return code.toString();
    }

    // 이메일 내용 초기화
    private String setContext(String code) {
        Context context = new Context();
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();

        context.setVariable("code", code);

        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false);

        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine.process("mail", context);
    }

    // 이메일 폼 생성
    private MimeMessage createEmailForm(String email) throws MessagingException {
        String authCode = createCode();
        log.info("Generated auth code: {}", authCode); // 생성된 인증 코드 로깅

        MimeMessage message = javaMailSender.createMimeMessage();
        message.addRecipients(MimeMessage.RecipientType.TO, email);
        message.setSubject("안녕하세요. 인증번호입니다.");
        message.setFrom(senderEmail);
        message.setText(setContext(authCode), "utf-8", "html");

        // Redis 에 해당 인증코드 인증 시간 설정
        // 밀리초를 초 단위로 변환 (1000으로 나눔)
        redisRepository.setDataExpire(email, authCode, authCodeExpirationMillis / 1000);

        return message;
    }

    // 인증코드 이메일 발송
    public void sendEmail(String toEmail) throws MessagingException {
        if (redisRepository.existData(toEmail)) {
            redisRepository.deleteData(toEmail);
        }
        // 이메일 폼 생성
        MimeMessage emailForm = createEmailForm(toEmail);
        // 이메일 발송
        javaMailSender.send(emailForm);
    }

    // 코드 검증
    public enum VerificationResult {
        SUCCESS,
        EXPIRED,
        INVALID
    }

    // 코드 검증
    public VerificationResult verifyEmailCode(String email, String code) {
        String codeFoundByEmail = redisRepository.getData(email);
        log.info("Verifying code for email: {}, Input code: {}, Stored code: {}", email, code, codeFoundByEmail);
        
        if (codeFoundByEmail == null) {
            return VerificationResult.EXPIRED;
        }
        
        return codeFoundByEmail.equals(code) ? VerificationResult.SUCCESS : VerificationResult.INVALID;
    }
}
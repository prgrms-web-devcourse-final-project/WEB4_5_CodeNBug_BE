package org.codeNbug.mainserver.domain.user.controller;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.domain.user.dto.EmailDto;
import org.codeNbug.mainserver.domain.user.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/email")
public class EmailController {
    private final EmailService emailService;

    // 인증코드 메일 발송
    @PostMapping("/send")
    public ResponseEntity<String> mailSend(@RequestBody EmailDto.SendRequest request) throws MessagingException {
        log.info("EmailController.mailSend()");
        emailService.sendEmail(request.getMail());
        return ResponseEntity.ok("인증코드가 발송되었습니다.");
    }

    // 인증코드 인증
    @PostMapping("/verify")
    public ResponseEntity<String> verify(@RequestBody EmailDto.VerifyRequest request) {
        log.info("EmailController.verify()");
        boolean isVerify = emailService.verifyEmailCode(request.getMail(), request.getVerifyCode());
        if (!isVerify) {
            return ResponseEntity.badRequest().body("인증에 실패했습니다.");
        }
        return ResponseEntity.ok("인증이 완료되었습니다.");
    }
}
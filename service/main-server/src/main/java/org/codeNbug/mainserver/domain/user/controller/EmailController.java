package org.codeNbug.mainserver.domain.user.controller;

import org.codeNbug.mainserver.domain.user.dto.EmailDto;
import org.codeNbug.mainserver.domain.user.service.email.EmailService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/email")
public class EmailController {
	private final EmailService emailService;

	// 인증코드 메일 발송
	@PostMapping("/send")
	public ResponseEntity<RsData<Void>> mailSend(@RequestBody EmailDto.SendRequest request) {
		log.info("EmailController.mailSend()");
		try {
			emailService.sendEmail(request.getMail());
			return ResponseEntity.ok(
				new RsData<>("200-SUCCESS", "인증코드가 발송되었습니다."));
		} catch (MessagingException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new RsData<>("500-INTERNAL_SERVER_ERROR", "이메일 발송에 실패했습니다."));
		}
	}

	// 인증코드 인증
	@PostMapping("/verify")
	public ResponseEntity<RsData<Void>> verify(@RequestBody EmailDto.VerifyRequest request) {
		log.info("EmailController.verify()");
		boolean isVerify = emailService.verifyEmailCode(request.getMail(), request.getVerifyCode());

		if (!isVerify) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new RsData<>("400-BAD_REQUEST", "인증에 실패했습니다."));
		}
		return ResponseEntity.ok(
			new RsData<>("200-SUCCESS", "인증이 완료되었습니다."));
	}
}
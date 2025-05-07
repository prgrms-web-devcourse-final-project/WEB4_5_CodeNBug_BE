package org.codeNbug.mainserver.external.toss.webhook.controller;

import org.codeNbug.mainserver.external.toss.webhook.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Toss Webhook 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/webhook/toss")
@RequiredArgsConstructor
public class WebhookController {

	private final WebhookService webhookService;

	@PostMapping("/status")
	public ResponseEntity<String> handleTossWebhook(@RequestBody String payload,
		@RequestHeader(value = "X-Toss-Signature", required = false) String signature) {
		log.info("signatrue:: {}", signature);
		try {
			webhookService.handleWebhook(payload, signature);
			return ResponseEntity.ok("웹훅 수신 완료");
		} catch (Exception e) {
			return ResponseEntity.status(400).body("웹훅 에러 " + e.getMessage() + "가 발생했습니다.");
		}
	}
}

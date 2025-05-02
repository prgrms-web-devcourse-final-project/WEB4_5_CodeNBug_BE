// package org.codeNbug.mainserver.external.toss.webhook.controller;
//
// import org.codeNbug.mainserver.external.toss.webhook.service.WebhookService;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestHeader;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;
//
// import lombok.RequiredArgsConstructor;
//
// /**
//  * Toss Webhook 컨트롤러
//  */
// @RestController
// @RequestMapping("/webhook/toss")
// @RequiredArgsConstructor
// public class WebhookController {
//
// 	private final WebhookService webhookService;
//
// 	@PostMapping("/status")
// 	public ResponseEntity<String> handleTossWebhook(@RequestBody String payload,
// 		@RequestHeader("X-Toss-Signature") String signature) {
// 		try {
// 			webhookService.processWebhook(payload, signature);
// 			return ResponseEntity.ok("웹훅을 받았습니다.");
// 		} catch (Exception e) {
// 			return ResponseEntity.status(400).body("웹훅 에러 " + e.getMessage() + "가 발생했습니다.");
// 		}
// 	}
// }

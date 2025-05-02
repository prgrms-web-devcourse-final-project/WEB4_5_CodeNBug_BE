package org.codeNbug.mainserver.domain.notification.dto;

import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 생성 요청을 위한 DTO 클래스
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateRequestDto {

	@NotNull(message = "사용자 ID는 필수입니다")
	private Long userId;

	@NotNull(message = "알림 유형은 필수입니다")
	private NotificationEnum type;

	@NotEmpty(message = "알림 내용은 필수입니다")
	private String content;
}
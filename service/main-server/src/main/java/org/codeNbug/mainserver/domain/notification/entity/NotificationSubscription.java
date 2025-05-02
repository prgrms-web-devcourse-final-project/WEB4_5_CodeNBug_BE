package org.codeNbug.mainserver.domain.notification.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationSubscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, unique = true)
	private String sessionId;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime subscribedAt;

	@Column
	private LocalDateTime lastActiveAt;

	@Builder
	public NotificationSubscription(Long userId, String sessionId) {
		this.userId = userId;
		this.sessionId = sessionId;
		this.lastActiveAt = LocalDateTime.now();
	}

	/**
	 * 마지막 활성 시간 업데이트
	 */
	public void updateLastActive() {
		this.lastActiveAt = LocalDateTime.now();
	}
}
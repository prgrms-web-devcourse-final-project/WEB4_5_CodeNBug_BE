package org.codeNbug.mainserver.domain.purchase.entity;

import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.user.entity.User;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Purchase 엔티티 클래스
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Purchase {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String paymentUuid;

	private LocalDateTime purchaseDate;

	private Integer amount;

	@Enumerated(EnumType.STRING)
	private PaymentMethodEnum paymentMethod;

	@Enumerated(EnumType.STRING)
	private PaymentStatusEnum paymentStatus;

	// TODO itemName -> 추후 변경 예정
	private String itemName;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
}

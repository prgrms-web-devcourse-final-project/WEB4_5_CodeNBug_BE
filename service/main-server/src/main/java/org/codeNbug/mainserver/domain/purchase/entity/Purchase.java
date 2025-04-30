package org.codeNbug.mainserver.domain.purchase.entity;

import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Purchase 엔티티 클래스
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Purchase {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String paymentUuid;

	private Integer amount;

	@Enumerated(EnumType.STRING)
	private PaymentMethodEnum paymentMethod;

	@Enumerated(EnumType.STRING)
	private PaymentStatusEnum paymentStatus;

	private String orderName;

	@CreatedDate
	private LocalDateTime purchaseDate;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	public void updatePaymentInfo(
		int amount,
		PaymentMethodEnum paymentMethod,
		PaymentStatusEnum paymentStatus,
		String orderName,
		LocalDateTime purchaseDate
	) {
		this.amount = amount;
		this.paymentMethod = paymentMethod;
		this.paymentStatus = paymentStatus;
		this.orderName = orderName;
		this.purchaseDate = purchaseDate;
	}
}

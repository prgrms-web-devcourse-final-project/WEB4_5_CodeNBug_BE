package org.codeNbug.mainserver.domain.purchase.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Purchase 엔티티 클래스
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@EntityListeners(AuditingEntityListener.class)
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

	@OneToMany(mappedBy = "purchase")
	private List<Ticket> tickets = new ArrayList<>();


	public void updatePaymentInfo(
		String paymentUuid,
		int amount,
		PaymentMethodEnum paymentMethod,
		PaymentStatusEnum paymentStatus,
		String orderName,
		LocalDateTime purchaseDate
	) {
		this.paymentUuid = paymentUuid;
		this.amount = amount;
		this.paymentMethod = paymentMethod;
		this.paymentStatus = paymentStatus;
		this.orderName = orderName;
		this.purchaseDate = purchaseDate;
	}
}

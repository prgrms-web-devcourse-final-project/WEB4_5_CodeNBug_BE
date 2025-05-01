package org.codeNbug.mainserver.domain.purchase.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", length = 50)
	private PaymentStatusEnum paymentStatus;

	private String orderId;

	@Setter
	private String orderName;

	@CreatedDate
	private LocalDateTime purchaseDate;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
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

	public void addTicket(Ticket ticket) {
		tickets.add(ticket);
		ticket.setPurchase(this);
	}
}

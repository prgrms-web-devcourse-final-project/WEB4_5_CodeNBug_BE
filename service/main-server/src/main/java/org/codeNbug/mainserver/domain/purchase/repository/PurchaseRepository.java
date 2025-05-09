package org.codeNbug.mainserver.domain.purchase.repository;

import java.util.List;
import java.util.Optional;

import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
	List<Purchase> findByUserUserIdAndPaymentStatusInOrderByPurchaseDateDesc(Long userId,
		List<PaymentStatusEnum> statuses);

	Optional<Purchase> findByPaymentUuid(String paymentKey);

	List<Purchase> findAllByEventId(Long eventId);
}

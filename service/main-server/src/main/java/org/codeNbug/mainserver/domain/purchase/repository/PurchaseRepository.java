package org.codeNbug.mainserver.domain.purchase.repository;

import java.util.List;

import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
	List<Purchase> findByUserUserIdAndPaymentStatusInOrderByPurchaseDateDesc(Long userId,
		List<PaymentStatusEnum> statuses);
}

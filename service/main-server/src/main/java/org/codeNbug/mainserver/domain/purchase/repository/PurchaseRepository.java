package org.codeNbug.mainserver.domain.purchase.repository;

import java.util.List;
import java.util.Optional;

import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
	List<Purchase> findByUserUserIdAndPaymentStatusInOrderByPurchaseDateDesc(Long userId,
		List<PaymentStatusEnum> statuses);

	Page<Purchase> findByUserUserIdAndPaymentStatusInOrderByPurchaseDateDesc(Long userId,
		List<PaymentStatusEnum> statuses, Pageable pageable);

	Optional<Purchase> findByPaymentUuid(String paymentKey);

	@Query("""
    SELECT DISTINCT p FROM Purchase p
    JOIN p.tickets t
    WHERE t.event.eventId = :eventId
""")
	List<Purchase> findAllByEventId(@Param("eventId") Long eventId);

}

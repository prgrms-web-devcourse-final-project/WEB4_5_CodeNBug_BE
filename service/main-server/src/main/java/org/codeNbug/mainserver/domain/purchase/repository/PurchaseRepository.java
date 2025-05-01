package org.codeNbug.mainserver.domain.purchase.repository;

import java.util.Optional;

import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
	Optional<Purchase> findById(Long purchaseId);
}

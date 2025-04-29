package org.codeNbug.mainserver.domain.purchase.repository;

import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
}

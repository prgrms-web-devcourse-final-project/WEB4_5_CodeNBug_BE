package org.codeNbug.mainserver.domain.purchase.repository;

import org.codeNbug.mainserver.domain.purchase.entity.PurchaseCancel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseCancelRepository extends JpaRepository<PurchaseCancel, Long> {
}

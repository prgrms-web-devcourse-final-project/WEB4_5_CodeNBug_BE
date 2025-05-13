package org.codeNbug.mainserver.domain.ticket.repository;

import java.util.List;

import org.codeNbug.mainserver.domain.manager.dto.TicketDto;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
	@Query("""
    SELECT new org.codeNbug.mainserver.domain.manager.dto.TicketDto(
        p.id, u.userId, u.name, u.email, u.phoneNum,
        p.paymentStatus, p.purchaseDate, p.amount, t.id
    )
    FROM Ticket t
    JOIN t.purchase p
    JOIN p.user u
    WHERE t.event.eventId = :eventId
""")
	List<TicketDto> findTicketPurchasesByEventId(@Param("eventId") Long eventId);


	List<Ticket> findAllByPurchaseId(Long purchaseId);
	
	@Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.eventId = :eventId")
	int countByEventId(@Param("eventId") Long eventId);
	
	@Query("SELECT COUNT(t) FROM Ticket t JOIN t.purchase p WHERE t.event.eventId = :eventId AND p.paymentStatus = org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum.DONE")
	int countPaidTicketsByEventId(@Param("eventId") Long eventId);
}

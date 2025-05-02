package org.codeNbug.mainserver.domain.ticket.repository;

import org.codeNbug.mainserver.domain.manager.dto.TicketDto;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Query("""
    SELECT new org.codeNbug.mainserver.domain.manager.dto.TicketDto(
        p.id, u.id, u.name, u.email, u.phoneNum,
        p.paymentStatus, p.purchaseDate, p.amount, t.id
    )
    FROM Ticket t
    JOIN t.purchase p
    JOIN p.user u
    WHERE t.event.id = :eventId
""")
    List<TicketDto> findTicketPurchasesByEventId(@Param("eventId") Long eventId);

}

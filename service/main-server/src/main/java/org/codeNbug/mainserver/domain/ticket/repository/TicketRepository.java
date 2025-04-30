package org.codeNbug.mainserver.domain.ticket.repository;

import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}

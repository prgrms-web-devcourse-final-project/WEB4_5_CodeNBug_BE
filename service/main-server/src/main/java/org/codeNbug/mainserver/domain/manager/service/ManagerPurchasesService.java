package org.codeNbug.mainserver.domain.manager.service;

import java.util.List;
import java.util.stream.Collectors;

import org.codeNbug.mainserver.domain.manager.dto.EventPurchaseResponse;
import org.codeNbug.mainserver.domain.manager.dto.TicketDto;
import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

// 1. 매니저가 행사에 대한 티켓 내역 조회기능
// 2. 매니저가 행사에 대한 티켓에 대한 환불 기능
@Service
@RequiredArgsConstructor
public class ManagerPurchasesService {

	private final TicketRepository ticketRepository;
	private final EventRepository eventRepository;
	private final ManagerEventRepository managerEventRepository;

	public List<EventPurchaseResponse> getEventPurchaseList(Long eventId, User currentUser) {
		validateManager(eventId, currentUser);
		List<TicketDto> flatList = ticketRepository.findTicketPurchasesByEventId(eventId);

		return flatList.stream()
			.collect(Collectors.groupingBy(TicketDto::getPurchaseId))
			.values()
			.stream()
			.map(group -> {
				TicketDto first = group.get(0);
				List<Long> ticketIds = group.stream()
					.map(TicketDto::getTicket_id)
					.toList();

				return new EventPurchaseResponse(
					first.getPurchaseId(),
					first.getUserId(),
					first.getUserName(),
					first.getUserEmail(),
					first.getPhoneNum(),
					first.getPayment_status(),
					ticketIds,
					first.getPurchaseAt(),
					first.getAmount()
				);
			})
			.toList();
	}

	private void validateManager(Long eventId, User currentUser) {
		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new BadRequestException("해당 이벤트를 찾을 수 없습니다."));
		boolean isManager = managerEventRepository.existsByManagerAndEvent(currentUser, event);
		if (!isManager) {
			throw new AccessDeniedException("해당 이벤트에 대한 권한이 없습니다.");
		}
	}
}

package org.codeNbug.mainserver.domain.manager.service;

import java.util.List;
import java.util.Map;

import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.entity.EventInformation;
import org.codeNbug.mainserver.domain.manager.entity.EventType;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.EventTypeRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.entity.User;
import org.codenbug.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventEditService {

	private final EventRepository eventRepository;
	private final SeatLayoutRepository seatLayoutRepository;
	private final SeatGradeRepository seatGradeRepository;
	private final SeatRepository seatRepository;
	private final EventDomainService eventDomainService;
	private final EventTypeRepository eventTypeRepository;
	private final ManagerEventRepository managerEventRepository;
	private final UserRepository userRepository;

	/**
	 * 이벤트 수정 메인 메서드입니다.
	 * managerId 검증부터 시작하여 Event 정보 및 좌석 정보를 모두 갱신합니다.
	 */
	@Transactional
	public EventRegisterResponse editEvent(Long eventId, EventRegisterRequest request, Long managerId) {
		Event event = getEventOrThrow(eventId);
		validateManagerAuthority(managerId, event);
		updateEventTypeIfChanged(event, request.getType());
		updateEventInformation(event, request);
		updateBookingPeriod(event, request);
		updateSeatLayout(eventId, request);
		updateSeatsAndGrades(event, request);

		return eventDomainService.buildEventRegisterResponse(request, event);
	}

	/**
	 * 이벤트를 ID로 조회하는 메서드입니다.
	 * 존재하지 않을 경우 예외를 발생시킵니다.
	 */
	private Event getEventOrThrow(Long eventId) {
		return eventRepository.findById(eventId)
			.orElseThrow(() -> new BadRequestException("이벤트를 찾을 수 없습니다: id=" + eventId));
	}

	/**
	 * 이벤트에 대한 수정 권한을 가진 매니저인지 검증하는 메서드입니다.
	 * 권한이 없으면 예외를 발생시킵니다.
	 */
	private void validateManagerAuthority(Long managerId, Event event) {
		User manager = userRepository.findById(managerId)
			.orElseThrow(() -> new BadRequestException("메니저를 찾을 수 없습니다."));
		boolean hasPermission = managerEventRepository.existsByManagerAndEvent(manager, event);
		if (!hasPermission) {
			throw new BadRequestException("해당 이벤트에 대한 수정 권한이 없습니다.");
		}
	}

	/**
	 * 요청한 타입이 기존과 다르면 EventType을 변경하는 메서드입니다.
	 */
	private void updateEventTypeIfChanged(Event event, String newTypeName) {
		String currentTypeName = eventTypeRepository.findById(event.getTypeId())
			.orElseThrow(() -> new BadRequestException("이벤트 타입을 찾을 수 없습니다."))
			.getName();

		if (!currentTypeName.equals(newTypeName)) {
			EventType eventType = eventDomainService.findOrCreateEventType(newTypeName);
			event.setTypeId(eventType.getEventTypeId());
		}
	}

	/**
	 * 이벤트 정보를 갱신하는 메서드입니다.
	 * EventInformation 객체를 새로 만들어 대체합니다.
	 */
	private void updateEventInformation(Event event, EventRegisterRequest request) {
		EventInformation newInformation = EventInformation.builder()
			.title(request.getTitle())
			.thumbnailUrl(request.getThumbnailUrl())
			.description(request.getDescription())
			.ageLimit(request.getAgelimit())
			.restrictions(request.getRestriction())
			.location(request.getLocation())
			.hallName(request.getHallName())
			.eventStart(request.getStartDate())
			.eventEnd(request.getEndDate())
			.seatCount(request.getSeatCount())
			.build();
		event.setInformation(newInformation);
	}

	/**
	 * 이벤트의 예약 시작/종료 기간을 수정하는 메서드입니다.
	 */
	private void updateBookingPeriod(Event event, EventRegisterRequest request) {
		event.setBookingStart(request.getBookingStart());
		event.setBookingEnd(request.getBookingEnd());
	}

	/**
	 * 좌석 레이아웃을 JSON 형태로 직렬화하고 업데이트하는 메서드입니다.
	 */
	private void updateSeatLayout(Long eventId, EventRegisterRequest request) {
		SeatLayout seatLayout = seatLayoutRepository.findByEvent_EventId(eventId)
			.orElseThrow(() -> new BadRequestException("좌석 레이아웃을 찾을 수 없습니다: eventId=" + eventId));
		String layoutJson = eventDomainService.serializeLayoutToJson(request.getLayout());
		seatLayout.setLayout(layoutJson);
	}

	/**
	 * 기존 좌석 및 좌석 등급 정보를 삭제하고, 새로 생성하는 메서드입니다.
	 */
	private void updateSeatsAndGrades(Event event, EventRegisterRequest request) {
		List<Seat> oldSeats = seatRepository.findByEvent(event);
		seatRepository.deleteAll(oldSeats);

		List<SeatGrade> oldGrades = seatGradeRepository.findByEvent(event);
		seatGradeRepository.deleteAll(oldGrades);

		Map<String, SeatGrade> seatGradeMap = eventDomainService.createAndSaveSeatGrades(event, request.getPrice());
		eventDomainService.createAndSaveSeats(event,
			seatLayoutRepository.findByEvent_EventId(event.getEventId()).orElseThrow(), request.getLayout(),
			seatGradeMap);
	}
}



package org.codeNbug.mainserver.domain.manager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.codeNbug.mainserver.domain.event.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.entity.ManagerEvent;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.service.SeatService;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventRegisterService {

	private final EventDomainService eventDomainService;
	private final EventRepository eventRepository;
	private final SeatLayoutRepository seatLayoutRepository;
	private final ManagerEventRepository managerEventRepository;
	private final UserRepository userRepository;
	private final SeatGradeRepository seatGradeRepository;
	private final SeatService seatService;

	/**
	 * 이벤트 등록 메인 메서드
	 */
	@Transactional
	public EventRegisterResponse registerEvent(EventRegisterRequest request, Long managerId) {
		Event event = createAndSaveEvent(request);
		SeatLayout seatLayout = createAndSaveSeatLayout(request, event);
		Map<String, SeatGrade> seatGradeMap = eventDomainService.createAndSaveSeatGrades(event, request.getPrice());
		eventDomainService.createAndSaveSeats(event, seatLayout, request.getLayout(), seatGradeMap);
		saveManagerEvent(managerId, event);
		List<Integer> priceList = seatGradeMap.values()
			.stream()
			.map(seatGrade -> seatGrade.getAmount())
			.sorted().toList();
		Integer minPrice = priceList.getFirst();
		Integer maxPrice = priceList.getLast();
		event.setSeatLayout(seatLayout);
		event.setMinPrice(minPrice);
		event.setMaxPrice(maxPrice);
		return eventDomainService.buildEventRegisterResponse(request, event);
	}

	/**
	 * 이벤트 등록 시 해당 이벤트와 등록한 매니저를 ManagerEvent 에 저장하는 메서드
	 * @param managerId
	 * @param event
	 */
	private void saveManagerEvent(Long managerId, Event event) {
		User manager = userRepository.findById(managerId)
			.orElseThrow(() -> new BadRequestException("존재하지 않는 매니저입니다."));

		managerEventRepository.save(new ManagerEvent(null, manager, event));
	}

	/**
	 * Event 생성 및 저장
	 */
	private Event createAndSaveEvent(EventRegisterRequest request) {

		Event event = new Event(
			request.getCategory(),
			EventInformation.builder()
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
				.build(),
			request.getBookingStart(),
			request.getBookingEnd(),
			0, // viewCount 기본값
			null, // createdAt
			null, // modifiedAt
			determineInitialEventStatus(request.getBookingStart(), request.getBookingEnd()),
			true,
			false,
			null
		);

		return eventRepository.save(event);
	}

	/**
	 * bookingStart, bookingEnd, 현재시간(now)을 비교하여 초기 상태를 결정
	 */
	private EventStatusEnum determineInitialEventStatus(LocalDateTime bookingStart, LocalDateTime bookingEnd) {
		LocalDateTime now = LocalDateTime.now();

		if (bookingStart != null && bookingEnd != null) {
			if (!now.isBefore(bookingStart) && !now.isAfter(bookingEnd)) {
				return EventStatusEnum.OPEN; // 예매 중
			} else {
				return EventStatusEnum.CLOSED; // 예매 종료
			}
		}
		return EventStatusEnum.CLOSED; // 날짜 미입력 → CLOSED
	}

	/**
	 * SeatLayout 생성 및 저장
	 * - 요청된 Layout 정보를 JSON으로 직렬화 후 저장
	 * - 저장된 SeatLayout은 해당 Event와 연결
	 *
	 * @param request 이벤트 등록 요청
	 * @param event 저장된 Event
	 * @return 저장된 SeatLayout
	 */
	private SeatLayout createAndSaveSeatLayout(EventRegisterRequest request, Event event) {
		String layoutJson = eventDomainService.serializeLayoutToJson(request.getLayout());
		SeatLayout seatLayout = new SeatLayout(
			null,
			layoutJson,
			event
		);
		seatService.evictSeatLayoutCache(event.getEventId());
		return seatLayoutRepository.save(seatLayout);
	}
}

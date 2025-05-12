package org.codeNbug.mainserver.domain.manager.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import org.codeNbug.mainserver.domain.event.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.PriceDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.SeatInfoDto;
import org.codeNbug.mainserver.domain.manager.entity.ManagerEvent;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.common.util.Util;
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
		event.setSeatLayout(seatLayout);
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
			EventStatusEnum.OPEN,
			true,
			false,
			null
		);

		return eventRepository.save(event);
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
		return seatLayoutRepository.save(seatLayout);
	}

	@Transactional(readOnly = true)
	public EventRegisterResponse searchEvent(Long eventId, Long currentUserId) {
		// 1. fetch join으로 조회 (seats까지 한 번에 초기화)
		Event event = eventRepository.findByIdWithSeats(eventId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

		// 2. 매니저 권한 검증
		eventDomainService.validateManagerAuthority(currentUserId, event);

		// 3. EventType 조회
		EventCategoryEnum category = event.getCategory();

		// 4. EventInformation
		EventInformation info = event.getInformation();

		// 5. SeatLayout → LayoutDto
		LayoutDto layoutDto = null;
		SeatLayout seatLayout = event.getSeatLayout();
		if (seatLayout != null) {
			// ✅ seatLayout.layout = {"layout": [...], "seat": {...}} 형태 → layout만 추출
			Map<String, Object> fullLayoutMap = Util.fromJson(
					seatLayout.getLayout(),
					new TypeReference<Map<String, Object>>() {}
			);

			Object layoutObj = fullLayoutMap.get("layout");
			List<List<String>> layoutRows = Util.convertValue(
					layoutObj,
					new TypeReference<List<List<String>>>() {}
			);

			Map<String, SeatInfoDto> seatMap = seatLayout.getSeats().stream()
					.collect(Collectors.toMap(
							Seat::getLocation,
							seat -> SeatInfoDto.builder()
									.grade(seat.getGrade().getGrade().name())
									.build()
					));

			layoutDto = LayoutDto.builder()
					.layout(layoutRows)
					.seat(seatMap)
					.build();
		}

		// 6. 가격 정보 → SeatGrade
		List<PriceDto> priceDtos = seatGradeRepository.findByEvent(event).stream()
				.map(seatGrade -> PriceDto.builder()
						.grade(seatGrade.getGrade().name())
						.amount(seatGrade.getAmount())
						.build())
				.collect(Collectors.toList());

		// 7. 최종 응답
		return EventRegisterResponse.builder()
				.eventId(event.getEventId())
				.title(info.getTitle())
				.category(category)
				.description(info.getDescription())
				.restriction(info.getRestrictions())
				.thumbnailUrl(info.getThumbnailUrl())
				.startDate(info.getEventStart())
				.endDate(info.getEventEnd())
				.location(info.getLocation())
				.hallName(info.getHallName())
				.seatCount(info.getSeatCount() != null ? info.getSeatCount() : 0)
				.layout(layoutDto)
				.price(priceDtos)
				.bookingStart(event.getBookingStart())
				.bookingEnd(event.getBookingEnd())
				.agelimit(info.getAgeLimit() != null ? info.getAgeLimit() : 0)
				.createdAt(event.getCreatedAt())
				.modifiedAt(event.getModifiedAt())
				.status(event.getStatus())
				.build();
	}
}

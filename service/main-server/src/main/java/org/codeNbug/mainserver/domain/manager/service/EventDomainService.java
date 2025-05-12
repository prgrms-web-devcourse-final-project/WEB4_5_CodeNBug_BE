package org.codeNbug.mainserver.domain.manager.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codeNbug.mainserver.domain.event.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventType;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.PriceDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.SeatInfoDto;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.EventTypeRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventDomainService {

	private final EventRepository eventRepository;
	private final ManagerEventRepository managerEventRepository;
	private final EventTypeRepository eventTypeRepository;
	private final SeatGradeRepository seatGradeRepository;
	private final SeatRepository seatRepository;
	private final ObjectMapper objectMapper;
	private final UserRepository userRepository;

	public EventType findOrCreateEventType(String typeName) {
		return eventTypeRepository.findByName(typeName)
			.orElseGet(() -> eventTypeRepository.save(new EventType(null, typeName)));
	}

	public String serializeLayoutToJson(LayoutDto layoutDto) {
		try {
			return objectMapper.writeValueAsString(layoutDto);
		} catch (Exception e) {
			throw new RuntimeException("좌석 레이아웃 직렬화 실패", e);
		}
	}

	public Map<String, SeatGrade> createAndSaveSeatGrades(Event event, List<PriceDto> prices) {
		Map<String, SeatGrade> seatGradeMap = new HashMap<>();
		for (PriceDto price : prices) {
			SeatGrade seatGrade = new SeatGrade(
				null,
				SeatGradeEnum.valueOf(price.getGrade()),
				price.getAmount(),
				event);
			SeatGrade savedGrade = seatGradeRepository.save(seatGrade);
			seatGradeMap.put(price.getGrade(), savedGrade);
		}
		return seatGradeMap;
	}

	public void createAndSaveSeats(Event event, SeatLayout seatLayout, LayoutDto layoutDto,
		Map<String, SeatGrade> seatGradeMap) {
		for (List<String> row : layoutDto.getLayout()) {
			for (String seatName : row) {
				if (seatName == null)
					continue;

				SeatInfoDto seatInfo = layoutDto.getSeat().get(seatName);
				if (seatInfo == null) {
					throw new BadRequestException("좌석 정보가 존재하지 않습니다: " + seatName);
				}

				SeatGrade seatGrade = seatGradeMap.get(seatInfo.getGrade());
				if (seatGrade == null) {
					throw new BadRequestException("좌석 등급 정보가 존재하지 않습니다: " + seatInfo.getGrade());
				}

				Seat seat = new Seat(
					null,
					seatName,
					true,
					seatGrade,
					seatLayout,
					null,
					event
				);

				seatRepository.save(seat);
			}
		}
	}


	/**
	 * 이벤트를 ID로 조회하는 메서드입니다.
	 * 존재하지 않을 경우 예외를 발생시킵니다.
	 */
	public Event getEventOrThrow(Long eventId) {
		return eventRepository.findById(eventId)
				.orElseThrow(() -> new BadRequestException("이벤트를 찾을 수 없습니다: id=" + eventId));
	}

	/**
	 * 이벤트에 대한 수정 권한을 가진 매니저인지 검증하는 메서드입니다.
	 * 권한이 없으면 예외를 발생시킵니다.
	 */
	public void validateManagerAuthority(Long managerId, Event event) {
		User manager = userRepository.findById(managerId)
				.orElseThrow(() -> new BadRequestException("메니저를 찾을 수 없습니다."));
		boolean hasPermission = managerEventRepository.existsByManagerAndEvent(manager, event);
		if (!hasPermission) {
			throw new BadRequestException("해당 이벤트에 대한 수정 권한이 없습니다.");
		}
	}

	/**
	 * 최종 응답 객체 생성
	 */
	public EventRegisterResponse buildEventRegisterResponse(EventRegisterRequest request, Event event) {
		return EventRegisterResponse.builder()
			.eventId(event.getEventId())
			.title(event.getInformation().getTitle())
			.type(request.getType())
			.description(request.getDescription())
			.restriction(request.getRestriction())
			.thumbnailUrl(event.getInformation().getThumbnailUrl())
			.startDate(event.getInformation().getEventStart())
			.endDate(event.getInformation().getEventEnd())
			.location(event.getInformation().getLocation())
			.hallName(event.getInformation().getHallName())
			.seatCount(event.getInformation().getSeatCount())
			.layout(request.getLayout())
			.price(request.getPrice())
			.bookingStart(event.getBookingStart())
			.bookingEnd(event.getBookingEnd())
			.agelimit(event.getInformation().getAgeLimit())
			.createdAt(event.getCreatedAt())
			.modifiedAt(event.getModifiedAt())
			.status(event.getStatus())
			.build();
	}
}

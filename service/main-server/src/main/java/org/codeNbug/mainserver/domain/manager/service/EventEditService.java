package org.codeNbug.mainserver.domain.manager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.codeNbug.mainserver.domain.event.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.service.NotificationService;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.seat.service.SeatService;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventEditService {

	private final SeatLayoutRepository seatLayoutRepository;
	private final SeatGradeRepository seatGradeRepository;
	private final SeatRepository seatRepository;
	private final EventDomainService eventDomainService;
	private final PurchaseRepository purchaseRepository;
	private final NotificationService notificationService;
	private final SeatService seatService;

	/**
	 * 이벤트 수정 메인 메서드입니다.
	 * managerId 검증부터 시작하여 Event 정보 및 좌석 정보를 모두 갱신합니다.
	 */
	@Transactional
	public EventRegisterResponse editEvent(Long eventId, EventRegisterRequest request, Long managerId) {
		Event event = eventDomainService.getEventOrThrow(eventId);
		eventDomainService.validateManagerAuthority(managerId, event);

		// 원래 이벤트 정보를 저장해 변경 여부 확인용으로 사용
		String originalTitle = event.getInformation().getTitle();
		String originalLocation = event.getInformation().getLocation();
		LocalDateTime originalStartDate = event.getInformation().getEventStart();
		LocalDateTime originalEndDate = event.getInformation().getEventEnd();

		updateEventCategoryIfChanged(event, request.getCategory());
		updateEventInformation(event, request);
		updateBookingPeriod(event, request);
		updateSeatLayout(eventId, request);
		updateSeatsAndGrades(event, request);

		// 이벤트 수정 알림 처리 추가
		try {
			// 해당 이벤트 구매자들 조회
			List<Purchase> purchases = purchaseRepository.findAllByEventId(eventId);

			// 중요 정보가 변경되었는지 확인
			boolean titleChanged = !originalTitle.equals(request.getTitle());
			boolean locationChanged = !originalLocation.equals(request.getLocation());
			boolean startTimeChanged = !originalStartDate.equals(request.getStartDate());
			boolean endTimeChanged = !originalEndDate.equals(request.getEndDate());

			// 중요 정보가 변경된 경우만 알림 전송
			if (titleChanged || locationChanged || startTimeChanged || endTimeChanged) {
				StringBuilder changes = new StringBuilder();
				if (titleChanged) {
					changes.append("공연명 변경 ");
				}
				if (locationChanged) {
					changes.append("장소 변경 ");
				}
				if (startTimeChanged) {
					changes.append("공연 시작 시간 변경 ");
				}
				if (endTimeChanged) {
					changes.append("공연 종료 시간 변경 ");
				}

				String notificationContent = String.format(
					"[%s] 행사 정보가 변경되었습니다. (%s)",
					request.getTitle(),
					changes.toString().trim());

				for (Purchase purchase : purchases) {
					try {
						Long userId = purchase.getUser().getUserId();
						notificationService.createNotification(
							userId,
							NotificationEnum.EVENT,
							notificationContent
						);
					} catch (Exception e) {
						log.error("행사 정보 변경 알림 전송 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
							purchase.getUser().getUserId(), purchase.getId(), e.getMessage(), e);
						// 개별 사용자 알림 실패는 이벤트 수정에 영향을 주지 않도록 예외를 무시함
					}
				}

				log.info("행사 정보 변경 알림 전송 완료. 이벤트ID: {}, 변경사항: {}, 대상자 수: {}",
					eventId, changes.toString().trim(), purchases.size());
			}
		} catch (Exception e) {
			log.error("행사 정보 변경 알림 처리 실패. 이벤트ID: {}, 오류: {}", eventId, e.getMessage(), e);
			// 알림 전체 실패는 이벤트 수정에 영향을 주지 않도록 예외를 무시함
		}

		return eventDomainService.buildEventRegisterResponse(request, event);
	}

	/**
	 * 요청한 타입이 기존과 다르면 EventType을 변경하는 메서드입니다.
	 */
	private void updateEventCategoryIfChanged(Event event, EventCategoryEnum newCategory) {
		if (!event.getCategory().equals(newCategory)) {
			event.setCategory(newCategory);
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
		seatService.evictSeatLayoutCache(eventId);
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



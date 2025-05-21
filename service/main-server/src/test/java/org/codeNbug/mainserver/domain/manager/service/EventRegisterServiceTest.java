package org.codeNbug.mainserver.domain.manager.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.codeNbug.mainserver.domain.event.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventRegisterServiceTest {

	@Mock
	private EventDomainService eventDomainService;
	@Mock
	private EventRepository eventRepository;
	@Mock
	private SeatLayoutRepository seatLayoutRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ManagerEventRepository managerEventRepository;

	@InjectMocks
	private EventRegisterService eventRegisterService;

	@DisplayName("이벤트 등록 성공 테스트")
	@Test
	void registerEvent_success() {
		// given
		Long managerId = 1L;
		User manager = User.builder()
			.userId(managerId)
			.email("test@example.com")
			.build();
		EventRegisterRequest request = createDummyRequest();

		Event event = new Event(
			EventCategoryEnum.CONCERT,
			EventInformation.builder()
				.title("Test Event")
				.description("Description")
				.ageLimit(0)
				.restrictions("")
				.location("Seoul")
				.hallName("Olympic Hall")
				.eventStart(LocalDateTime.now())
				.eventEnd(LocalDateTime.now().plusHours(2))
				.seatCount(100)
				.thumbnailUrl("https://image.com/test.jpg")
				.build(),
			LocalDateTime.now(),
			LocalDateTime.now().plusDays(1),
			0,
			null,
			null,
			EventStatusEnum.OPEN,
			true,
			false,
			null
		);

		SeatLayout seatLayout = new SeatLayout(1L, "{}", event);

		// when mocking
		when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));
		when(eventRepository.save(any(Event.class))).thenReturn(event);
		when(eventDomainService.serializeLayoutToJson(any())).thenReturn("{}");
		when(seatLayoutRepository.save(any(SeatLayout.class))).thenReturn(seatLayout);
		when(eventDomainService.createAndSaveSeatGrades(any(), any())).thenReturn(Map.of(
			"A1", new SeatGrade(1L, SeatGradeEnum.A, 1000, null), "A2", new SeatGrade(1L, SeatGradeEnum.A, 2000, null)
		));
		when(eventDomainService.buildEventRegisterResponse(any(), any()))
			.thenReturn(createDummyResponse());

		// when
		EventRegisterResponse response = eventRegisterService.registerEvent(request, managerId);

		// then
		assertNotNull(response);
		assertEquals("Test Event", response.getTitle());
		assertEquals("Seoul", response.getLocation());
	}

	// 더미 요청 생성 메서드
	private EventRegisterRequest createDummyRequest() {
		return EventRegisterRequest.builder()
			.title("Test Event")
			.category(EventCategoryEnum.CONCERT)
			.thumbnailUrl("https://image.com/test.jpg")
			.description("Description")
			.agelimit(0)
			.restriction("")
			.location("Seoul")
			.hallName("Olympic Hall")
			.startDate(LocalDateTime.now())
			.endDate(LocalDateTime.now().plusHours(2))
			.seatCount(100)
			.bookingStart(LocalDateTime.now())
			.bookingEnd(LocalDateTime.now().plusDays(1))
			.layout(LayoutDto.builder()
				.layout(Collections.emptyList())
				.seat(Collections.emptyMap())
				.build())
			.price(Collections.emptyList())
			.build();
	}

	// 더미 응답 생성 메서드
	private EventRegisterResponse createDummyResponse() {
		return EventRegisterResponse.builder()
			.eventId(1L)
			.title("Test Event")
			.category(EventCategoryEnum.CONCERT)
			.description("Description")
			.restriction("")
			.thumbnailUrl("https://image.com/test.jpg")
			.startDate(LocalDateTime.now())
			.endDate(LocalDateTime.now().plusHours(2))
			.location("Seoul")
			.hallName("Olympic Hall")
			.seatCount(100)
			.layout(LayoutDto.builder()
				.layout(Collections.emptyList())
				.seat(Collections.emptyMap())
				.build())
			.price(Collections.emptyList())
			.bookingStart(LocalDateTime.now())
			.bookingEnd(LocalDateTime.now().plusDays(1))
			.agelimit(0)
			.createdAt(LocalDateTime.now())
			.modifiedAt(LocalDateTime.now())
			.status(EventStatusEnum.OPEN)
			.build();
	}
}


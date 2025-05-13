package org.codeNbug.mainserver.domain.seat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.seat.dto.SeatLayoutResponse;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class SeatServiceTest {

	@InjectMocks
	private SeatService seatService;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private SeatLayoutRepository seatLayoutRepository;

	@Mock
	private RedisLockService redisLockService;

	private Long userId;
	private Long eventId;
	private SeatLayout seatLayout;
	private Event event;
	private Seat seat1, seat2;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		userId = 1L;
		eventId = 100L;

		Event event = new Event();
		ReflectionTestUtils.setField(event, "eventId", eventId);

		seatLayout = SeatLayout.builder()
			.id(10L)
			.event(event)
			.layout("{\"layout\":[[\"A1\",\"A2\"]],\"seat\":{\"A1\":{\"grade\":\"VIP\"},\"A2\":{\"grade\":\"R\"}}}")
			.build();

		SeatGrade vipGrade = SeatGrade.builder()
			.id(1L)
			.grade(SeatGradeEnum.VIP)
			.amount(100000)
			.event(event)
			.build();

		SeatGrade rGrade = SeatGrade.builder()
			.id(2L)
			.grade(SeatGradeEnum.R)
			.amount(80000)
			.event(event)
			.build();

		seat1 = Seat.builder()
			.id(1L)
			.location("A1")
			.grade(vipGrade)
			.available(true)
			.layout(seatLayout)
			.event(event)
			.build();

		seat2 = Seat.builder()
			.id(2L)
			.location("A2")
			.grade(rGrade)
			.available(false)
			.layout(seatLayout)
			.event(event)
			.build();
	}

	@Test
	@DisplayName("좌석 조회 성공")
	void getSeatLayout_success() {
		// given
		given(seatLayoutRepository.findByEvent_EventId(eventId)).willReturn(Optional.of(seatLayout));
		given(seatRepository.findAllByLayoutIdWithGrade(seatLayout.getId())).willReturn(List.of(seat1, seat2));

		// when
		SeatLayoutResponse result = seatService.getSeatLayout(eventId, userId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getSeats()).hasSize(2);
		assertThat(result.getLayout()).isNotEmpty();
		assertThat(result.getSeats().get(0).getLocation()).isEqualTo("A1");
	}

	@Test
	@DisplayName("좌석 조회 실패 - 존재하지 않는 이벤트")
	void getSeatLayout_eventNotFound() {
		given(seatLayoutRepository.findByEvent_EventId(eventId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> seatService.getSeatLayout(eventId, userId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 이벤트에 좌석 레이아웃이 존재하지 않습니다.");
	}
}
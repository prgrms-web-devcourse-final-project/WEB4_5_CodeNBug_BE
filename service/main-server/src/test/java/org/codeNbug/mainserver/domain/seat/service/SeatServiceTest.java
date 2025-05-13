package org.codeNbug.mainserver.domain.seat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.seat.dto.SeatLayoutResponse;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectResponse;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codeNbug.mainserver.global.exception.globalException.ConflictException;
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
	private EventRepository eventRepository;

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

		event = new Event();
		ReflectionTestUtils.setField(event, "eventId", eventId);
		ReflectionTestUtils.setField(event, "seatSelectable", true); // 이걸로 예매 모드 설정

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
			.available(true)
			.layout(seatLayout)
			.event(event)
			.build();
	}

	@Test
	@DisplayName("좌석 조회 성공")
	void getSeatLayout_success() {
		// given
		given(seatLayoutRepository.findByEvent_EventId(eventId)).willReturn(Optional.of(seatLayout));
		given(seatRepository.findAllByLayoutIdWithGrade(seatLayout.getId()))
			.willReturn(List.of(seat1, seat2));

		// when
		SeatLayoutResponse result = seatService.getSeatLayout(eventId, userId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getSeats()).hasSize(2);
		assertThat(result.getLayout()).isNotEmpty();
		assertThat(result.getSeats().get(0).getLocation()).isEqualTo("A1");
		assertThat(result.getSeats().get(1).getLocation()).isEqualTo("A2");
	}

	@Test
	@DisplayName("좌석 조회 실패 - 존재하지 않는 이벤트")
	void getSeatLayout_eventNotFound() {
		// given
		given(seatLayoutRepository.findByEvent_EventId(eventId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> seatService.getSeatLayout(eventId, userId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 이벤트에 좌석 레이아웃이 존재하지 않습니다.");
	}

	@Test
	@DisplayName("지정석 선택 성공")
	void selectSeat_success() {
		// given
		given(eventRepository.findById(eventId)).willReturn(Optional.of(event));

		SeatSelectRequest request = new SeatSelectRequest(List.of(1L, 2L), 2);

		given(seatRepository.findById(1L)).willReturn(Optional.of(seat1));
		given(seatRepository.findById(2L)).willReturn(Optional.of(seat2));

		given(redisLockService.tryLock(any(), any(), any())).willReturn(true);

		// when
		SeatSelectResponse result = seatService.selectSeat(eventId, request, userId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getSeatList()).containsExactly(1L, 2L);
	}

	@Test
	@DisplayName("지정석 선택 실패 - 이미 선택된 좌석")
	void selectSeat_fail_alreadyReserved() {
		// given
		ReflectionTestUtils.setField(event, "seatSelectable", true);
		given(eventRepository.findById(eventId)).willReturn(Optional.of(event));

		Seat reservedSeat = Seat.builder()
			.id(3L)
			.location("A3")
			.grade(seat1.getGrade())
			.available(false) // 예약됨
			.layout(seatLayout)
			.event(event)
			.build();

		SeatSelectRequest request = new SeatSelectRequest(List.of(3L), 1);

		given(seatRepository.findById(3L)).willReturn(Optional.of(reservedSeat));

		// when & then
		assertThatThrownBy(() -> seatService.selectSeat(eventId, request, userId))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("이미 예매된 좌석입니다");
	}

	@Test
	@DisplayName("지정석 선택 실패 - 좌석 수 4개 초과")
	void selectSeat_fail_tooManySeats() {
		// given
		ReflectionTestUtils.setField(event, "seatSelectable", true);
		given(eventRepository.findById(eventId)).willReturn(Optional.of(event));

		SeatSelectRequest request = new SeatSelectRequest(List.of(1L, 2L, 3L, 4L, 5L), 5);

		// when & then
		assertThatThrownBy(() -> seatService.selectSeat(eventId, request, userId))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("최대 4개의 좌석만 선택할 수 있습니다.");
	}

	@Test
	@DisplayName("미지정석 선택 성공")
	void nonSelectSeat_success() {
		// given
		ReflectionTestUtils.setField(event, "seatSelectable", false); // 미지정석

		given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
		given(seatRepository.findAvailableSeatsByEventId(eventId)).willReturn(List.of(seat1, seat2));
		given(redisLockService.tryLock(any(), any(), any())).willReturn(true);

		SeatSelectRequest request = new SeatSelectRequest(null, 2); // 미지정석 예매 시 좌석 목록은 null

		// when
		SeatSelectResponse result = seatService.selectSeat(eventId, request, userId);

		// then
		assertThat(result).isNotNull();
	}

	@Test
	@DisplayName("미지정석 선택 실패 - 좌석 목록 전달")
	void nonSelectSeat_withSeats_fail() {
		// given
		ReflectionTestUtils.setField(event, "seatSelectable", false); // 미지정석

		given(eventRepository.findById(eventId)).willReturn(Optional.of(event));

		// 좌석 목록이 전달된 경우 예외 발생
		SeatSelectRequest request = new SeatSelectRequest(List.of(1L, 2L), 2);

		// when & then
		assertThatThrownBy(() -> seatService.selectSeat(eventId, request, userId))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("[selectSeats] 미지정석 예매 시 좌석 목록은 제공되지 않아야 합니다.");
	}

	@Test
	@DisplayName("미지정석 선택 실패 - 좌석 부족")
	void nonSelectSeat_insufficientSeats_fail() {
		// given
		ReflectionTestUtils.setField(event, "seatSelectable", false); // 미지정석
		given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
		given(seatRepository.findAvailableSeatsByEventId(eventId)).willReturn(List.of(seat1));

		// 미지정석 예매 시 좌석 목록은 null
		SeatSelectRequest request = new SeatSelectRequest(null, 2);

		// when & then
		assertThatThrownBy(() -> seatService.selectSeat(eventId, request, userId))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("[selectSeats] 예매 가능한 좌석 수가 부족합니다.");
	}
}
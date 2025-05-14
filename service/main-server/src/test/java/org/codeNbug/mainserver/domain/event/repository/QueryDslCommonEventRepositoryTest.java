package org.codeNbug.mainserver.domain.event.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.global.config.QueryDslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@ComponentScan(basePackages = {"org.codeNbug.mainserver.domain.event.repository"})
@EntityScan(basePackages = {"org.codeNbug.mainserver.domain", "org.codenbug.user.domain"})
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class QueryDslCommonEventRepositoryTest {

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql =
		new MySQLContainer<>("mysql:8.0.34")
			.withDatabaseName("ticketoneTest")
			.withUsername("test")
			.withPassword("test");

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private SeatLayoutRepository seatLayoutRepository;

	@Autowired
	private SeatGradeRepository seatGradeRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private QueryDslCommonEventRepository queryDslCommonEventRepository;

	@Test
	@DisplayName("예매 가능한 좌석 조회 테스트")
	void countAvailableTest() {

		// 임의의 행사를 db에 저장
		EventInformation eventInformation = EventInformation.builder()
			.title("Test Event")
			.description("Test Description")
			.location("Test Location")
			.hallName("Test Hall")
			.eventStart(LocalDateTime.now().plusDays(10))
			.eventEnd(LocalDateTime.now().plusDays(11))
			.seatCount(10)
			.build();

		Event event = new Event(
			EventCategoryEnum.CONCERT,
			eventInformation,
			LocalDateTime.now(),
			LocalDateTime.now().plusDays(5),
			0,
			LocalDateTime.now(),
			LocalDateTime.now(),
			EventStatusEnum.OPEN,
			true,
			false,
			null
		);

		Event savedEvent = eventRepository.save(event);

		// Create SeatLayout
		SeatLayout seatLayout = SeatLayout.builder()
			.layout("Test Layout")
			.event(savedEvent)
			.build();

		SeatLayout savedSeatLayout = seatLayoutRepository.save(seatLayout);
		savedEvent.setSeatLayout(savedSeatLayout);
		eventRepository.save(savedEvent);

		// Create SeatGrade
		SeatGrade seatGrade = SeatGrade.builder()
			.grade(SeatGradeEnum.VIP)
			.amount(10000)
			.event(savedEvent)
			.build();

		SeatGrade savedSeatGrade = seatGradeRepository.save(seatGrade);

		// Create 10 seats
		for (int i = 0; i < 10; i++) {
			Seat seat = Seat.builder()
				.location("Seat " + i)
				.available(true)
				.grade(savedSeatGrade)
				.layout(savedSeatLayout)
				.event(savedEvent)
				.build();

			seatRepository.save(seat);
		}

		// 저장한 행사의 seat 2개를 골라 available을 false로 설정
		List<Seat> seats = seatRepository.findAll();
		seats.get(0).setAvailable(false);
		seats.get(1).setAvailable(false);
		seatRepository.saveAll(seats);

		// 예매 가능 좌석 조회 메소드 실행
		Integer availableSeatCount = queryDslCommonEventRepository.countAvailableSeat(savedEvent.getEventId());

		// 총 행사의 seat - 2가 나와야 함
		assertEquals(8, availableSeatCount);
	}
}

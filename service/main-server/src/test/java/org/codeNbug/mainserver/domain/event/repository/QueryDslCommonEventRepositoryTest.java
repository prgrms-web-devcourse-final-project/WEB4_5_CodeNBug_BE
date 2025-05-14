package org.codeNbug.mainserver.domain.event.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.entity.CostRange;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.event.entity.Location;
import org.codeNbug.mainserver.domain.event.entity.QEvent;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.seat.entity.QSeat;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.querydsl.core.Tuple;

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

	@Test
	@DisplayName("findAllByFilter 테스트")
	void findAllByFilterTest() {
		// Arrange: Create multiple events with different attributes
		Event event1 = createEvent("Concert A", EventCategoryEnum.CONCERT, EventStatusEnum.OPEN, "Location A", 1000,
			LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(11));
		Event event2 = createEvent("Concert B", EventCategoryEnum.CONCERT, EventStatusEnum.OPEN, "Location B", 2000,
			LocalDateTime.now().plusDays(15), LocalDateTime.now().plusDays(16));
		Event event3 = createEvent("Theater A", EventCategoryEnum.FAN_MEETING, EventStatusEnum.CLOSED, "Location A",
			500,
			LocalDateTime.now().plusDays(20), LocalDateTime.now().plusDays(21));

		// Act: Create an EventListFilter for filtering open concerts at "Location A" within a cost range and date range
		EventListFilter filter = new EventListFilter.Builder()
			.eventCategoryList(List.of(EventCategoryEnum.CONCERT))
			.eventStatusList(List.of(EventStatusEnum.OPEN))
			.locationList(List.of(new Location("Location A")))
			.costRange(new CostRange(500, 1500))
			.startDate(LocalDateTime.now().plusDays(5))
			.endDate(LocalDateTime.now().plusDays(15))
			.build();

		Pageable pageable = PageRequest.of(0, 10);
		Page<Tuple> result = queryDslCommonEventRepository.findAllByFilter(filter, pageable);

		// Assert: Verify only "Concert A" is returned as it satisfies the filter criteria
		assertEquals(1, result.getTotalElements());
		Tuple tuple = result.getContent().get(0);
		Event filteredEvent = tuple.get(QEvent.event);
		assertEquals("Concert A", filteredEvent.getInformation().getTitle());
		assertEquals(1000, tuple.get(QSeat.seat.grade.amount.min().as("minPrice")));
		assertEquals(1000, tuple.get(QSeat.seat.grade.amount.max().as("maxPrice")));
	}

	private Event createEvent(String title, EventCategoryEnum category, EventStatusEnum status, String location,
		int price, LocalDateTime start, LocalDateTime end) {
		EventInformation eventInfo = EventInformation.builder()
			.title(title)
			.description("Test description")
			.location(location)
			.hallName("Test Hall")
			.eventStart(start)
			.eventEnd(end)
			.seatCount(10)
			.build();

		Event event = new Event(
			category,
			eventInfo,
			start,
			end,
			0,
			LocalDateTime.now(),
			LocalDateTime.now(),
			status,
			false,
			false,
			null
		);
		Event savedEvent = eventRepository.save(event);

		// Create SeatLayout and SeatGrade
		SeatLayout seatLayout = SeatLayout.builder()
			.layout("Standard Layout")
			.event(savedEvent)
			.build();
		SeatLayout savedSeatLayout = seatLayoutRepository.save(seatLayout);

		SeatGrade seatGrade = SeatGrade.builder()
			.grade(SeatGradeEnum.VIP)
			.amount(price)
			.event(savedEvent)
			.build();
		seatGradeRepository.save(seatGrade);

		// Create seats
		for (int i = 0; i < 10; i++) {
			Seat seat = Seat.builder()
				.location("Seat " + i)
				.available(true)
				.grade(seatGrade)
				.layout(savedSeatLayout)
				.event(savedEvent)
				.build();
			seatRepository.save(seat);
		}
		savedEvent.setSeatLayout(savedSeatLayout);

		return savedEvent;
	}

	@Test
	@DisplayName("findAllByFilterAndKeyword 테스트")
	void findAllByFilterAndKeywordTest() {
		// Arrange: Create multiple events with various attributes
		Event event1 = createEvent("Concert Alpha", EventCategoryEnum.CONCERT, EventStatusEnum.OPEN, "Location Alpha",
			1500,
			LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(11));
		Event event2 = createEvent("Concert Beta", EventCategoryEnum.CONCERT, EventStatusEnum.OPEN, "Location Beta",
			2000,
			LocalDateTime.now().plusDays(15), LocalDateTime.now().plusDays(16));
		Event event3 = createEvent("Theater Gamma", EventCategoryEnum.FAN_MEETING, EventStatusEnum.CLOSED,
			"Location Gamma",
			500, LocalDateTime.now().plusDays(20), LocalDateTime.now().plusDays(21));

		// Act: Create an EventListFilter to filter open concerts within a specific cost range and date range
		EventListFilter filter = new EventListFilter.Builder()
			.eventCategoryList(List.of(EventCategoryEnum.CONCERT))
			.eventStatusList(List.of(EventStatusEnum.OPEN))
			.costRange(new CostRange(1000, 1800))
			.startDate(LocalDateTime.now().plusDays(5))
			.endDate(LocalDateTime.now().plusDays(15))
			.build();

		// Search for events with the title keyword "Alpha"
		Pageable pageable = PageRequest.of(0, 10);
		Page<Tuple> result = queryDslCommonEventRepository.findAllByFilterAndKeyword("Alpha", filter, pageable);

		// Assert: Verify that only "Concert Alpha" is returned as it meets the filter and keyword criteria
		assertEquals(1, result.getTotalElements());
		Tuple tuple = result.getContent().get(0);
		Event filteredEvent = tuple.get(QEvent.event);
		assertEquals("Concert Alpha", filteredEvent.getInformation().getTitle());
		assertEquals(1500, tuple.get(QSeat.seat.grade.amount.min().as("minPrice")));
		assertEquals(1500, tuple.get(QSeat.seat.grade.amount.max().as("maxPrice")));
	}

	@Test
	@DisplayName("findAllByKeyword - 단어 필터링 테스트")
	void newFindAllByKeywordTest() {
		// Arrange: Create several events with different attributes
		Event event1 = createEvent("Rock Concert", EventCategoryEnum.CONCERT, EventStatusEnum.OPEN, "Big Arena", 1200,
			LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(11));
		Event event2 = createEvent("Classical Night", EventCategoryEnum.CONCERT, EventStatusEnum.OPEN, "Opera Hall",
			1500,
			LocalDateTime.now().plusDays(12), LocalDateTime.now().plusDays(13));
		Event event3 = createEvent("Rock Legends Festival", EventCategoryEnum.CONCERT, EventStatusEnum.CLOSED,
			"Outdoor Stage", 2000, LocalDateTime.now().plusDays(20), LocalDateTime.now().plusDays(21));

		// Act: Use the "findAllByKeyword" method to search for events with the keyword "Rock"
		Pageable pageable = PageRequest.of(0, 10);
		Page<Tuple> result = queryDslCommonEventRepository.findAllByKeyword("Rock", pageable);

		// Assert: Verify that only events containing "Rock" in the title are returned
		assertEquals(2, result.getTotalElements());

		Tuple tuple1 = result.getContent().get(0);
		Tuple tuple2 = result.getContent().get(1);

		Event filteredEvent1 = tuple1.get(QEvent.event);
		Event filteredEvent2 = tuple2.get(QEvent.event);

		assertTrue(filteredEvent1.getInformation().getTitle().contains("Rock"));
		assertTrue(filteredEvent2.getInformation().getTitle().contains("Rock"));
	}

	private void addSeatGrade(Event event, SeatGradeEnum gradeType, int price, SeatLayout layout) {
		SeatGrade seatGrade = SeatGrade.builder()
			.grade(gradeType)
			.amount(price)
			.event(event)
			.build();
		seatGrade = seatGradeRepository.save(seatGrade);

		for (int i = 0; i < 5; i++) {
			Seat seat = Seat.builder()
				.location("Seat " + gradeType + i)
				.available(true)
				.grade(seatGrade)
				.layout(layout)
				.event(event)
				.build();
			seatRepository.save(seat);
		}
	}

	@Test
	@DisplayName("행사의 티켓의 최소값과 최대값을 반환해야 한다")
	void testMultiplePricedSeats() {
		// Create event with basic setup
		Event event = createEvent("Multi Price Concert", EventCategoryEnum.CONCERT, EventStatusEnum.OPEN,
			"Test Location", 1000, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

		// Add additional seat grade with different price
		addSeatGrade(event, SeatGradeEnum.VIP, 2000, event.getSeatLayout());

		// Search for the event
		Pageable pageable = PageRequest.of(0, 10);
		Page<Tuple> result = queryDslCommonEventRepository.findAllByKeyword("Multi Price", pageable);

		assertEquals(1, result.getTotalElements());
		Tuple tuple = result.getContent().get(0);

		// Verify min and max prices
		assertEquals(1000, tuple.get(1, Integer.class));
		assertEquals(2000, tuple.get(2, Integer.class));
	}

}

package org.codeNbug.mainserver.domain.event.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.codeNbug.mainserver.domain.event.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.CostRange;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.event.entity.Location;
import org.codeNbug.mainserver.domain.event.repository.JpaCommonEventRepository;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.PriceDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.SeatInfoDto;
import org.codeNbug.mainserver.util.TestUtil;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.transaction.Transactional;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class EventControllerTest {

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql =
		new MySQLContainer<>("mysql:8.0.34")
			.withDatabaseName("ticketoneTest")
			.withUsername("test")
			.withPassword("test");
	@Container
	@ServiceConnection
	static GenericContainer<?> redis =
		new GenericContainer<>("redis:alpine")
			.withExposedPorts(6379);


	// 2) 스프링 프로퍼티에 컨테이너 URL/계정 주입
	@DynamicPropertySource
	static void overrideProps(DynamicPropertyRegistry registry) {

		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.redis.host", () -> redis.getHost());
		registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	private ObjectMapper objectMapper = new ObjectMapper()
		.registerModule(new JavaTimeModule())
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	@Autowired
	private JpaCommonEventRepository jpaCommonEventRepository;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@BeforeEach
	void setUp() {
		TestUtil.truncateAllTables(jdbcTemplate);
	}

	@Test
	@DisplayName("이벤트 목록 조회 테스트 - 필터와 keyword 없이")
	void getEvents() throws Exception {
		// given
		// 매니저 생성 및 인증정보 넣음
		TestUtil.createManagerAndSaveAuthentication(userRepository);

		// event 생성
		EventRegisterResponse event1 = TestUtil.registerEvent(mockMvc, "title1", objectMapper);
		EventRegisterResponse event2 = TestUtil.registerEvent(mockMvc, "title2", objectMapper);
		EventRegisterResponse event3 = TestUtil.registerEvent(mockMvc, "title3", objectMapper);

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/events"));

		// then
		String eventString = result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("event list 조회 성공."))
			.andReturn().getResponse().getContentAsString();
		List<EventListResponse> list = new ArrayList<>();
		objectMapper.readTree(eventString).get("data").forEach(node -> {
			list.add(objectMapper.convertValue(node, EventListResponse.class));
		});

		Assertions.assertThat(list)
			.extracting("eventId").contains(event1.getEventId(), event2.getEventId(), event3.getEventId());
	}

	@Test
	@DisplayName("이벤트 목록 조회 테스트 - 가격 범위 필터만 사용")
	void getEventsByCostRange() throws Exception {
		// given
		TestUtil.createManagerAndSaveAuthentication(userRepository);
		LayoutDto layoutDto = LayoutDto.builder()
			.layout(List.of(List.of("A1", "A2"), List.of("B1", "B2")))
			.seat(Map.of(
				"A1", new SeatInfoDto("S"),
				"A2", new SeatInfoDto("A"),
				"B1", new SeatInfoDto("B"),
				"B2", new SeatInfoDto("R")
			))
			.build();
		// 최소 티켓 금액이 3000인 이벤트 생성
		List<PriceDto> priceList1 = List.of(
			new PriceDto("S", 10000),
			new PriceDto("A", 8000),
			new PriceDto("B", 5000),
			new PriceDto("R", 3000)
		);
		EventRegisterResponse event1 = TestUtil.registerEvent(mockMvc, layoutDto, priceList1, objectMapper);

		// 최소 티켓 금액이 5000인 이벤트 생성
		List<PriceDto> priceList2 = List.of(
			new PriceDto("S", 10000),
			new PriceDto("A", 12000),
			new PriceDto("B", 10000),
			new PriceDto("R", 5000)
		);
		EventRegisterResponse event2 = TestUtil.registerEvent(mockMvc, layoutDto, priceList2, objectMapper);

		// 최소 티켓 금액이 8000인 이벤트 생성
		List<PriceDto> priceList3 = List.of(
			new PriceDto("S", 10000),
			new PriceDto("A", 12000),
			new PriceDto("B", 10000),
			new PriceDto("R", 8000)
		);
		EventRegisterResponse event3 = TestUtil.registerEvent(mockMvc, layoutDto, priceList3, objectMapper);

		// 0~3000 사이의 가격을 가진 티켓이 있는 행사를 조회하는 필터
		EventListFilter filter = new EventListFilter.Builder().costRange(
			new CostRange(0, 3000)
		).build();

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/events")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(filter)));

		// then
		// 1번 event가 3000원인 티켓을 판매하므로 1번 이벤트만 포함되어야 함
		String eventString = result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("event list 조회 성공."))
			.andReturn().getResponse().getContentAsString();

		List<EventListResponse> list = new ArrayList<>();
		objectMapper.readTree(eventString).get("data").forEach(node -> {
			list.add(objectMapper.convertValue(node, EventListResponse.class));
		});

		Assertions.assertThat(list.size()).isEqualTo(1);

		Assertions.assertThat(list)
			.extracting("eventId")
			.containsExactly(event1.getEventId());
	}

	@Test
	@DisplayName("이벤트 목록 조회 테스트 - 위치(location) 필터만 사용")
	void getEventsByLocation() throws Exception {
		// given
		TestUtil.createManagerAndSaveAuthentication(userRepository);

		// 서울 지역 이벤트 생성
		EventRegisterResponse event1 = TestUtil.registerEvent(mockMvc, new Location("서울"), objectMapper);
		// 부산 지역 이벤트 생성
		EventRegisterResponse event2 = TestUtil.registerEvent(mockMvc, new Location("부산"), objectMapper);
		// 대구 지역 이벤트 생성
		EventRegisterResponse event3 = TestUtil.registerEvent(mockMvc, new Location("대구"), objectMapper);

		// 서울, 부산, 대구 지역 조회하는 필터
		EventListFilter filter = new EventListFilter.Builder().locationList(
			List.of(new Location("서울"), new Location("부산"))
		).build();

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/events")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(filter)));

		// then
		String eventString = result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("event list 조회 성공."))
			.andReturn().getResponse().getContentAsString();

		List<EventListResponse> list = new ArrayList<>();
		objectMapper.readTree(eventString).get("data").forEach(node -> {
			list.add(objectMapper.convertValue(node, EventListResponse.class));
		});

		Assertions.assertThat(list.size()).isEqualTo(2);
		Assertions.assertThat(list)
			.extracting("eventId")
			.contains(event1.getEventId())
			.contains(event2.getEventId());
	}

	@Test
	@DisplayName("이벤트 목록 조회 테스트 - 이벤트 타입 필터만 사용")
	void getEventsByEventType() throws Exception {
		// given
		TestUtil.createManagerAndSaveAuthentication(userRepository);

		// CONCERT 타입 이벤트 생성
		EventRegisterResponse event1 = TestUtil.registerEvent(mockMvc, "title1", "CONCERT", objectMapper);
		// SPORTS 타입 이벤트 생성
		EventRegisterResponse event2 = TestUtil.registerEvent(mockMvc, "title2", "SPORTS", objectMapper);
		// MOVIE 타입 이벤트 생성
		EventRegisterResponse event3 = TestUtil.registerEvent(mockMvc, "title3", "ETC", objectMapper);

		List<EventCategoryEnum> eventTypeIdToFilterList = Arrays.asList(EventCategoryEnum.values())
			.stream()
			.filter(type -> type.name().equals("CONCERT")
				|| type.name().equals("SPORTS")
			)
			.toList();

		// CONCERT, SPORTS 타입 조회하는 필터
		EventListFilter filter = new EventListFilter.Builder().eventCategoryList(
			eventTypeIdToFilterList
		).build();

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/events")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(filter)));

		// then
		String eventString = result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("event list 조회 성공."))
			.andReturn().getResponse().getContentAsString();

		List<EventListResponse> list = new ArrayList<>();
		objectMapper.readTree(eventString).get("data").forEach(node -> {
			list.add(objectMapper.convertValue(node, EventListResponse.class));
		});

		Assertions.assertThat(list.size()).isEqualTo(2);
		Assertions.assertThat(list)
			.extracting("eventId")
			.contains(event1.getEventId())
			.contains(event2.getEventId());
	}

	@Test
	@DisplayName("이벤트 목록 조회 테스트 - 이벤트 상태 필터만 사용")
	void getEventsByStatus() throws Exception {
		// given
		TestUtil.createManagerAndSaveAuthentication(userRepository);

		// OPEN 상태 이벤트 생성
		EventRegisterResponse event1 = TestUtil.registerEvent(mockMvc, "title1", objectMapper);
		Event openEvent = jpaCommonEventRepository.findById(event1.getEventId()).orElseThrow();
		openEvent.setStatus(EventStatusEnum.OPEN);
		jpaCommonEventRepository.save(openEvent);

		// CLOSED 상태 이벤트 생성
		EventRegisterResponse event2 = TestUtil.registerEvent(mockMvc, "title2", objectMapper);
		Event closedEvent = jpaCommonEventRepository.findById(event2.getEventId()).orElseThrow();
		closedEvent.setStatus(EventStatusEnum.CLOSED);
		jpaCommonEventRepository.save(closedEvent);

		// OPEN 상태만 조회하는 필터
		EventListFilter filter = new EventListFilter.Builder()
			.eventStatusList(List.of(EventStatusEnum.OPEN))
			.build();

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/events")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(filter)));

		// then
		String eventString = result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("event list 조회 성공."))
			.andReturn().getResponse().getContentAsString();

		List<EventListResponse> list = new ArrayList<>();
		objectMapper.readTree(eventString).get("data").forEach(node -> {
			list.add(objectMapper.convertValue(node, EventListResponse.class));
		});

		Assertions.assertThat(list.size()).isEqualTo(1);
		Assertions.assertThat(list)
			.extracting("eventId")
			.containsExactly(event1.getEventId());
	}

	@Test
	@DisplayName("이벤트 목록 조회 테스트 - 날짜 범위 필터만 사용")
	void getEventsByDateRange() throws Exception {
		// given
		TestUtil.createManagerAndSaveAuthentication(userRepository);

		// 이벤트 생성
		EventRegisterResponse event1 = TestUtil.registerEvent(mockMvc, "title1", objectMapper);
		Event event = jpaCommonEventRepository.findById(event1.getEventId()).orElseThrow();
		jpaCommonEventRepository.save(event);

		// 날짜 범위로 필터링
		EventListFilter filter = new EventListFilter.Builder()
			.startDate(LocalDateTime.now().minusDays(2))
			.endDate(LocalDateTime.now().plusDays(2))
			.build();

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/events")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(filter)));

		// then
		String eventString = result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("event list 조회 성공."))
			.andReturn().getResponse().getContentAsString();

		List<EventListResponse> list = new ArrayList<>();
		objectMapper.readTree(eventString).get("data").forEach(node -> {
			list.add(objectMapper.convertValue(node, EventListResponse.class));
		});

		Assertions.assertThat(list.size()).isEqualTo(1);
		Assertions.assertThat(list)
			.extracting("eventId")
			.containsExactly(event1.getEventId());
	}

	@Test
	@DisplayName("이벤트 목록 조회 테스트 - keyword 검색")
	void getEventsByKeyword() throws Exception {
		// given
		TestUtil.createManagerAndSaveAuthentication(userRepository);

		// 이벤트 생성
		EventRegisterResponse event1 = TestUtil.registerEvent(mockMvc, "콘서트", objectMapper);
		EventRegisterResponse event2 = TestUtil.registerEvent(mockMvc, "영화", objectMapper);
		EventRegisterResponse event3 = TestUtil.registerEvent(mockMvc, "콘서트2", objectMapper);

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/events?keyword=%s".formatted("콘서트"))
			.contentType(MediaType.APPLICATION_JSON));

		// then
		String eventString = result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("event list 조회 성공."))
			.andReturn().getResponse().getContentAsString();

		List<EventListResponse> list = new ArrayList<>();
		objectMapper.readTree(eventString).get("data").forEach(node -> {
			list.add(objectMapper.convertValue(node, EventListResponse.class));
		});

		Assertions.assertThat(list.size()).isEqualTo(2);
		Assertions.assertThat(list)
			.extracting("eventId")
			.contains(event1.getEventId(), event3.getEventId());
	}

	@Test
	@DisplayName("이벤트 카테고리 조회 테스트")
	void getEventCategories() throws Exception {
		// given
		// 이벤트 추가
		TestUtil.createManagerAndSaveAuthentication(userRepository);

		EventRegisterResponse event1 = TestUtil.registerEvent(mockMvc, "title1", "CONCERT", objectMapper);
		EventRegisterResponse event2 = TestUtil.registerEvent(mockMvc, "title2", "SPORTS", objectMapper);
		EventRegisterResponse event3 = TestUtil.registerEvent(mockMvc, "title3", "ETC", objectMapper);

		// when
		ResultActions result = mockMvc.perform(get("/api/v1/events/categories")
			.contentType(MediaType.APPLICATION_JSON));

		// then
		String eventCategoriesRetrievedSuccessfully = result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("Event categories retrieved successfully"))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(EventCategoryEnum.values().length))
			.andReturn().getResponse().getContentAsString();

		List<EventCategoryEnum> contents = objectMapper.convertValue(
			objectMapper.readTree(eventCategoriesRetrievedSuccessfully).get("data"),
			new TypeReference<List<EventCategoryEnum>>() {
			});
		Assertions.assertThat(contents).contains(EventCategoryEnum.CONCERT, EventCategoryEnum.SPORTS);

	}

	@Test
	@DisplayName("이벤트 좌석 수 조회 테스트")
	@Transactional
	void getAvailableSeatCount() throws Exception {
		//given
		TestUtil.createManagerAndSaveAuthentication(userRepository);

		EventRegisterResponse event1 = TestUtil.registerEvent(mockMvc, "title1", "CONCERT", objectMapper);

		// when

		// 유저가 1개의 좌석을 구매했다고 가정
		Event event = jpaCommonEventRepository.findById(event1.getEventId())
			.orElseThrow();
		event.getSeatLayout().getSeats().get(0).setAvailable(false);
		jpaCommonEventRepository.save(event);

		ResultActions result = mockMvc.perform(get("/api/v1/events/%d/seats".formatted(event1.getEventId()))
			.contentType(MediaType.APPLICATION_JSON));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("가능한 좌석수 조회 성공"))
			.andExpect(jsonPath("$.data").value(event1.getSeatCount()));
	}

	@Test
	@DisplayName("존재하지 않는 이벤트의 좌석 수 조회 테스트")
	void getAvailableSeatCountNotFound() throws Exception {
		//given
		TestUtil.createManagerAndSaveAuthentication(userRepository);
		Long nonExistentEventId = 999L;

		//when
		ResultActions result = mockMvc.perform(get("/api/v1/events/%d/seats".formatted(nonExistentEventId))
			.contentType(MediaType.APPLICATION_JSON));

		//then
		result.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404-NOT_FOUND"))
			.andExpect(jsonPath("$.msg").value("해당 id의 event는 없습니다."));
	}

	@Test
	@DisplayName("이벤트 상세 조회 테스트")
	void getEvent() throws Exception {
		//given
		TestUtil.createManagerAndSaveAuthentication(userRepository);

		EventRegisterResponse event1 = TestUtil.registerEvent(mockMvc, "title1", "CONCERT", objectMapper);

		// when
		ResultActions result = mockMvc.perform(get("/api/v1/events/%d".formatted(event1.getEventId()))
			.contentType(MediaType.APPLICATION_JSON));

		// then

		Event event = jpaCommonEventRepository.findById(event1.getEventId())
			.orElseThrow();
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("event 단건 조회 성공."))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.eventId").value(event1.getEventId()))
			.andExpect(jsonPath("$.data.information.title").value(event1.getTitle()))
			.andExpect(jsonPath("$.data.category").value(event.getCategory().name()));
	}

	@Test
	@DisplayName("존재하지 않는 이벤트 상세 조회 테스트")
	void getEventNotFound() throws Exception {
		//given
		TestUtil.createManagerAndSaveAuthentication(userRepository);
		Long nonExistentEventId = 999L;

		//when
		ResultActions result = mockMvc.perform(get("/api/v1/events/%d".formatted(nonExistentEventId))
			.contentType(MediaType.APPLICATION_JSON));

		//then
		result.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404-NOT_FOUND"))
			.andExpect(jsonPath("$.msg").value("해당 id의 event는 없습니다."));
	}

	@Test
	@DisplayName("이벤트 조회수 업데이트 테스트")
	void updateViewCount() throws Exception {
		// redis의 값을 초기화
		Set<String> keys = redisTemplate.keys("viewCount:*");
		if (keys != null && !keys.isEmpty()) {
			redisTemplate.delete(keys);
		}

		//given
		TestUtil.createManagerAndSaveAuthentication(userRepository);

		EventRegisterResponse event1 = TestUtil.registerEvent(mockMvc, "title1", "CONCERT", objectMapper);

		// when
		// 조회
		mockMvc.perform(get("/api/v1/events/%d".formatted(event1.getEventId()))
			.contentType(MediaType.APPLICATION_JSON));

		ResultActions result = mockMvc.perform(patch("/api/v1/events/view"));

		// then
		Event event = jpaCommonEventRepository.findById(event1.getEventId())
			.orElseThrow();
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("업데이트 성공"));
		Assertions.assertThat(event.getViewCount()).isEqualTo(1);
	}
}

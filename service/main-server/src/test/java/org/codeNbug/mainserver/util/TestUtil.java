package org.codeNbug.mainserver.util;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.codeNbug.mainserver.domain.event.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.event.entity.Location;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.PriceDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.SeatInfoDto;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.security.service.CustomUserDetails;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtil {

	public static void createManagerAndSaveAuthentication(UserRepository userRepository) {
		User manager = User.builder()
			.email("manager@test.com")
			.password("encrypted")
			.name("테스트 매니저")
			.sex("M")
			.age(30)
			.phoneNum("010-1234-5678")
			.role(UserRole.MANAGER.getAuthority()) // "ROLE_MANAGER"
			.build();

		userRepository.save(manager);

		// 👇 CustomUserDetails 생성
		CustomUserDetails userDetails = new CustomUserDetails(manager);

		// 👇 인증 토큰 생성 및 SecurityContext에 등록
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			userDetails, null, userDetails.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	public static EventRegisterResponse registerEvent(MockMvc mockMvc, String title, ObjectMapper objectMapper) throws
		Exception {
		LayoutDto layoutDto = LayoutDto.builder()
			.layout(List.of(List.of("A1", "A2"), List.of("B1", "B2")))
			.seat(Map.of(
				"A1", new SeatInfoDto("S"),
				"A2", new SeatInfoDto("S"),
				"B1", new SeatInfoDto("A"),
				"B2", new SeatInfoDto("A")
			))
			.build();

		EventRegisterRequest request = EventRegisterRequest.builder()
			.title(title)
			.type("CONCERT")
			.description("설명")
			.restriction("없음")
			.thumbnailUrl("https://example.com/image.jpg")
			.startDate(LocalDateTime.now().plusDays(5))
			.endDate(LocalDateTime.now().plusDays(7))
			.location("서울시 강남구")
			.hallName("1관")
			.seatCount(4)
			.layout(layoutDto)
			.price(List.of(
				new PriceDto("S", 100000),
				new PriceDto("A", 80000)
			))
			.bookingStart(LocalDateTime.now().plusDays(1))
			.bookingEnd(LocalDateTime.now().plusDays(4))
			.agelimit(12)
			.build();

		String perform = mockMvc.perform(post("/api/v1/manager/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andReturn().getResponse().getContentAsString();

		return objectMapper.convertValue(objectMapper.readTree(perform).get("data"), EventRegisterResponse.class);
	}

	public static EventRegisterResponse registerEvent(MockMvc mockMvc, LocalDateTime startDate, LocalDateTime endDate,
		ObjectMapper objectMapper) throws
		Exception {
		LayoutDto layoutDto = LayoutDto.builder()
			.layout(List.of(List.of("A1", "A2"), List.of("B1", "B2")))
			.seat(Map.of(
				"A1", new SeatInfoDto("S"),
				"A2", new SeatInfoDto("S"),
				"B1", new SeatInfoDto("A"),
				"B2", new SeatInfoDto("A")
			))
			.build();

		EventRegisterRequest request = EventRegisterRequest.builder()
			.title("title")
			.type("CONCERT")
			.description("설명")
			.restriction("없음")
			.thumbnailUrl("https://example.com/image.jpg")
			.startDate(startDate)
			.endDate(LocalDateTime.now().plusDays(7))
			.location("서울시 강남구")
			.hallName("1관")
			.seatCount(4)
			.layout(layoutDto)
			.price(List.of(
				new PriceDto("S", 100000),
				new PriceDto("A", 80000)
			))
			.bookingStart(LocalDateTime.now().plusDays(1))
			.bookingEnd(endDate)
			.agelimit(12)
			.build();

		String perform = mockMvc.perform(post("/api/v1/manager/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andReturn().getResponse().getContentAsString();

		return objectMapper.convertValue(objectMapper.readTree(perform).get("data"), EventRegisterResponse.class);
	}

	public static EventRegisterResponse registerEvent(MockMvc mockMvc, LayoutDto layoutDto, List<PriceDto> priceDtos,
		ObjectMapper objectMapper) throws Exception {
		EventRegisterRequest request = EventRegisterRequest.builder()
			.title("title")
			.type("CONCERT")
			.description("설명")
			.restriction("없음")
			.thumbnailUrl("https://example.com/image.jpg")
			.startDate(LocalDateTime.now().plusDays(5))
			.endDate(LocalDateTime.now().plusDays(7))
			.location("서울시 강남구")
			.hallName("1관")
			.seatCount(4)
			.layout(layoutDto)
			.price(priceDtos)
			.bookingStart(LocalDateTime.now().plusDays(1))
			.bookingEnd(LocalDateTime.now().plusDays(4))
			.agelimit(12)
			.build();

		String perform = mockMvc.perform(post("/api/v1/manager/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andReturn().getResponse().getContentAsString();

		return objectMapper.convertValue(objectMapper.readTree(perform).get("data"), EventRegisterResponse.class);
	}

	public static EventRegisterResponse registerEvent(MockMvc mockMvc, Location location,
		ObjectMapper objectMapper) throws Exception {
		LayoutDto layoutDto = LayoutDto.builder()
			.layout(List.of(List.of("A1", "A2"), List.of("B1", "B2")))
			.seat(Map.of(
				"A1", new SeatInfoDto("S"),
				"A2", new SeatInfoDto("S"),
				"B1", new SeatInfoDto("A"),
				"B2", new SeatInfoDto("A")
			))
			.build();
		EventRegisterRequest request = EventRegisterRequest.builder()
			.title("title")
			.type("CONCERT")
			.description("설명")
			.restriction("없음")
			.thumbnailUrl("https://example.com/image.jpg")
			.startDate(LocalDateTime.now().plusDays(5))
			.endDate(LocalDateTime.now().plusDays(7))
			.location(location.getLocation())
			.hallName("1관")
			.seatCount(4)
			.layout(layoutDto)
			.price(List.of(
				new PriceDto("S", 100000),
				new PriceDto("A", 80000)
			))
			.bookingStart(LocalDateTime.now().plusDays(1))
			.bookingEnd(LocalDateTime.now().plusDays(4))
			.agelimit(12)
			.build();

		String perform = mockMvc.perform(post("/api/v1/manager/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andReturn().getResponse().getContentAsString();

		return objectMapper.convertValue(objectMapper.readTree(perform).get("data"), EventRegisterResponse.class);
	}

	public static EventRegisterResponse registerEvent(MockMvc mockMvc, String title, String eventType,
		ObjectMapper objectMapper) throws
		Exception {
		LayoutDto layoutDto = LayoutDto.builder()
			.layout(List.of(List.of("A1", "A2"), List.of("B1", "B2")))
			.seat(Map.of(
				"A1", new SeatInfoDto("S"),
				"A2", new SeatInfoDto("S"),
				"B1", new SeatInfoDto("A"),
				"B2", new SeatInfoDto("A")
			))
			.build();

		EventRegisterRequest request = EventRegisterRequest.builder()
			.title(title)
			.type(eventType)
			.description("설명")
			.restriction("없음")
			.thumbnailUrl("https://example.com/image.jpg")
			.startDate(LocalDateTime.now().plusDays(5))
			.endDate(LocalDateTime.now().plusDays(7))
			.location("서울시 강남구")
			.hallName("1관")
			.seatCount(4)
			.layout(layoutDto)
			.price(List.of(
				new PriceDto("S", 100000),
				new PriceDto("A", 80000)
			))
			.bookingStart(LocalDateTime.now().plusDays(1))
			.bookingEnd(LocalDateTime.now().plusDays(4))
			.agelimit(12)
			.build();

		String perform = mockMvc.perform(post("/api/v1/manager/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andReturn().getResponse().getContentAsString();

		return objectMapper.convertValue(objectMapper.readTree(perform).get("data"), EventRegisterResponse.class);
	}

	public static void truncateAllTables(JdbcTemplate jdbcTemplate) {
		jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
		getTables(jdbcTemplate).forEach(tableName ->
			jdbcTemplate.execute("TRUNCATE TABLE " + tableName));
		jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
	}

	private static List<String> getTables(JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.queryForList(
			"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'ticketoneTest'",
			String.class
		);
	}
}

package org.codeNbug.mainserver.domain.seat.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.codeNbug.mainserver.domain.seat.dto.SeatLayoutResponse;
import org.codeNbug.mainserver.domain.seat.service.RedisLockService;
import org.codeNbug.mainserver.domain.seat.service.SeatService;
import org.codeNbug.mainserver.global.Redis.entry.EntryTokenValidator;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.security.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(SeatController.class)
@Import(SeatControllerTest.MockBeans.class)
class SeatControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	// private static StringRedisTemplate redisTemplate;

	@Autowired
	private SeatService seatService;

	@Autowired
	private RedisLockService redisLockService;

	@Autowired
	private EntryTokenValidator entryTokenValidator;

	@TestConfiguration
	static class MockBeans {
		@Bean
		public SeatService seatService() {
			return Mockito.mock(SeatService.class);
		}

		@Bean
		public RedisLockService redisLockService() {
			return Mockito.mock(RedisLockService.class);
		}

		@Bean
		public EntryTokenValidator entryTokenValidator() {
			return Mockito.mock(EntryTokenValidator.class);
		}
	}

	@BeforeEach
	void setUpSecurityContext() {
		User user = User.builder()
			.userId(1L)
			.email("test@codenbug.org")
			.password("encodedPw")
			.name("테스트유저")
			.sex("M")
			.phoneNum("01012345678")
			.location("서울시")
			.role("ROLE_USER")
			.age(25)
			.build();

		CustomUserDetails userDetails = new CustomUserDetails(user);

		Authentication auth = new UsernamePasswordAuthenticationToken(
			userDetails, null, userDetails.getAuthorities()
		);

		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	// @AfterAll
	// static void tearDown() {
	// 	Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().flushAll();
	// }

	@Test
	@DisplayName("좌석 조회 성공 - 200 반환")
	void getSeats_success() throws Exception {
		// given
		List<SeatLayoutResponse.SeatDto> seatList = List.of(
			new SeatLayoutResponse.SeatDto(1L, "A1", "VIP", true),
			new SeatLayoutResponse.SeatDto(4L, "B2", "VIP", false)
		);

		List<List<String>> layout = List.of(
			List.of("A1", "A2"),
			List.of("B1", "B2")
		);

		SeatLayoutResponse response = new SeatLayoutResponse(seatList, layout);

		// eventId에 대한 stubbing
		Long eventId = 1L;

		// 실제 로직을 타지 않게
		given(seatService.getSeatLayout(eq(eventId), anyLong()))
			.willReturn(response);

		// when & then
		MvcResult result = mockMvc.perform(get("/api/v1/event/{eventId}/seats", eventId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.msg").value("좌석 조회 성공"))
			.andExpect(jsonPath("$.data.seats[0].seatId").value(1))
			.andExpect(jsonPath("$.data.seats[0].location").value("A1"))
			.andExpect(jsonPath("$.data.seats[0].grade").value("VIP"))
			.andExpect(jsonPath("$.data.seats[0].available").value(true))
			.andExpect(jsonPath("$.data.seats[1].seatId").value(4))
			.andExpect(jsonPath("$.data.seats[1].location").value("B2"))
			.andExpect(jsonPath("$.data.seats[1].grade").value("VIP"))
			.andExpect(jsonPath("$.data.seats[1].available").value(false))
			.andExpect(jsonPath("$.data.layout[0][0]").value("A1"))
			.andExpect(jsonPath("$.data.layout[1][1]").value("B2"))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("좌석 조회 실패 - 존재하지 않는 행사 404 반환")
	void getSeats_eventNotFound_fail() throws Exception {
		// given
		Long invalidEventId = 999L;

		given(seatService.getSeatLayout(eq(invalidEventId), anyLong()))
			.willThrow(new IllegalArgumentException("행사가 존재하지 않습니다."));

		// when & then
		MvcResult result = mockMvc.perform(get("/api/v1/event/{eventId}/seats", invalidEventId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404-NOT_FOUND"))
			.andExpect(jsonPath("$.msg").value("행사가 존재하지 않습니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	// @Test
	// @DisplayName("좌석 선택 - 성공")
	// void selectSeats_success() throws Exception {
	// 	// given
	// 	SeatSelectRequest request = new SeatSelectRequest();
	// 	request.setSeatList(List.of(101L, 102L));
	// 	request.setTicketCount(2);
	//
	// 	// eventId에 대한 stubbing
	// 	Long eventId = 2L;
	//
	// 	SeatSelectResponse response = new SeatSelectResponse(List.of(101L, 102L));
	// 	given(seatService.selectSeat(eventId, any(SeatSelectRequest.class), anyLong()))
	// 		.willReturn(response);
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/event/{eventId}/seats", eventId)
	// 			.header("entryAuthToken", "testToken")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.code").value(200))
	// 		.andExpect(jsonPath("$.msg").value("좌석 선택 성공"));
	// }
	//
	// @Test
	// @DisplayName("좌석 취소 - 성공")
	// void cancelSeats_success() throws Exception {
	// 	// given
	// 	SeatCancelRequest request = new SeatCancelRequest();
	// 	request.setSeatList(List.of(101L));
	//
	// 	given(seatService.cancelSeat(anyLong(), any(SeatCancelRequest.class), anyString()))
	// 		.willReturn(new ApiResponse<>(200, "좌석 취소 성공", null));
	//
	// 	// when & then
	// 	mockMvc.perform(delete("/api/v1/event/1/seats")
	// 			.header("Authorization", "Bearer testToken")
	// 			.header("entryAuthToken", "testToken")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.code").value(200))
	// 		.andExpect(jsonPath("$.msg").value("좌석 취소 성공"));
	// }
}
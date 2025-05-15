package org.codeNbug.mainserver.domain.seat.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.codeNbug.mainserver.domain.seat.dto.SeatCancelRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatLayoutResponse;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectResponse;
import org.codeNbug.mainserver.domain.seat.service.SeatService;
import org.codeNbug.mainserver.global.Redis.entry.EntryTokenValidator;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codeNbug.mainserver.global.exception.globalException.ConflictException;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@ComponentScan(
		excludeFilters = @ComponentScan.Filter(
				type = FilterType.ASSIGNABLE_TYPE,
				classes = org.codeNbug.mainserver.global.config.BatchConfig.class
		)
)
@WebMvcTest(SeatController.class)
@Import(SeatControllerTest.MockBeans.class)
class SeatControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private SeatService seatService;

	@Autowired
	private EntryTokenValidator entryTokenValidator;

	@TestConfiguration
	static class MockBeans {
		@Bean
		public SeatService seatService() {
			return Mockito.mock(SeatService.class);
		}

		@Bean(name = "seatEntryTokenValidator")
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

		// 실제 로직을 타지 않게
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

	@Test
	@DisplayName("지정석 선택 성공 - 200 반환")
	void selectSeats_success() throws Exception {
		// given
		SeatSelectRequest request = new SeatSelectRequest();
		request.setSeatList(List.of(101L, 102L));
		request.setTicketCount(2);
		Long eventId = 2L;

		// 실제 로직을 타지 않게
		SeatSelectResponse response = new SeatSelectResponse(List.of(101L, 102L));
		given(seatService.selectSeat(eq(eventId), any(SeatSelectRequest.class), anyLong()))
			.willReturn(response);
		willDoNothing().given(entryTokenValidator).validate(anyLong(), anyString());

		// when & then
		MvcResult result = mockMvc.perform(post("/api/v1/event/{eventId}/seats", eventId)
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.msg").value("좌석 선택 성공"))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("지정석 선택 실패 - 좌석 5개 이상 선택 시 400 반환")
	void selectSeats_seatCountExceed_fail() throws Exception {
		// given
		Long eventId = 1L;
		SeatSelectRequest request = new SeatSelectRequest();
		request.setSeatList(List.of(1L, 2L, 3L, 4L, 5L));
		request.setTicketCount(5);

		given(seatService.selectSeat(eq(eventId), any(SeatSelectRequest.class), anyLong()))
			.willThrow(new BadRequestException("최대 4개의 좌석만 선택할 수 있습니다."));

		// when & then
		MvcResult result = mockMvc.perform(post("/api/v1/event/{eventId}/seats", eventId)
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400-BAD_REQUEST"))
			.andExpect(jsonPath("$.msg").value("최대 4개의 좌석만 선택할 수 있습니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("좌석 선택 실패 - 존재하지 않는 행사일 경우 404 반환")
	void selectSeats_eventNotFound_fail() throws Exception {
		// given
		Long invalidEventId = 999L;
		SeatSelectRequest request = new SeatSelectRequest();
		request.setSeatList(List.of(1L, 2L));
		request.setTicketCount(2);

		given(seatService.selectSeat(eq(invalidEventId), any(SeatSelectRequest.class), anyLong()))
			.willThrow(new IllegalArgumentException("행사가 존재하지 않습니다."));

		// when & then
		MvcResult result = mockMvc.perform(post("/api/v1/event/{eventId}/seats", invalidEventId)
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404-NOT_FOUND"))
			.andExpect(jsonPath("$.msg").value("행사가 존재하지 않습니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("좌석 선택 실패 - entryAuthToken 유효성 검증 실패의 경우 400 반환")
	void nonSelectSeats_fail_invalidEntryToken() throws Exception {
		// given
		SeatSelectRequest request = new SeatSelectRequest();
		request.setSeatList(List.of());
		request.setTicketCount(2);
		Long eventId = 2L;

		willThrow(new BadRequestException("잘못된 입장 토큰입니다."))
			.given(entryTokenValidator).validate(anyLong(), anyString());

		// when & then
		MvcResult result = mockMvc.perform(post("/api/v1/event/{eventId}/seats", eventId)
				.header("entryAuthToken", "invalidToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.msg").value("잘못된 입장 토큰입니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("미지정석 선택 성공 - 200 반환")
	void nonSelectSeats_success() throws Exception {
		// given
		SeatSelectRequest request = new SeatSelectRequest();
		request.setSeatList(List.of()); // 미지정석
		request.setTicketCount(2);

		Long eventId = 2L;

		SeatSelectResponse response = new SeatSelectResponse(List.of(201L, 202L));
		given(seatService.selectSeat(eq(eventId), any(SeatSelectRequest.class), anyLong()))
			.willReturn(response);
		willDoNothing().given(entryTokenValidator).validate(anyLong(), anyString());

		// when & then
		MvcResult result = mockMvc.perform(post("/api/v1/event/{eventId}/seats", eventId)
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.msg").value("좌석 선택 성공"))
			.andExpect(jsonPath("$.data.seatList").isArray())
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("미지정석 선택 실패 - 미지정석인데 좌석 목록이 전달된 경우 400 반환")
	void selectSeats_unselectableEventWithSeatList_fail() throws Exception {
		// given
		Long eventId = 3L;
		SeatSelectRequest request = new SeatSelectRequest();
		request.setSeatList(List.of(1L, 2L));
		request.setTicketCount(2);

		given(seatService.selectSeat(eq(eventId), any(SeatSelectRequest.class), anyLong()))
			.willThrow(new BadRequestException("[selectSeats] 미지정석 예매 시 좌석 목록은 제공되지 않아야 합니다."));
		willDoNothing().given(entryTokenValidator).validate(anyLong(), anyString());

		// when & then
		MvcResult result = mockMvc.perform(post("/api/v1/event/{eventId}/seats", eventId)
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400-BAD_REQUEST"))
			.andExpect(jsonPath("$.msg").value("[selectSeats] 미지정석 예매 시 좌석 목록은 제공되지 않아야 합니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("미지정석 선택 실패 - 예매 가능한 좌석 부족 시 409 반환")
	void selectSeats_notEnoughAvailableSeats_fail() throws Exception {
		// given
		Long eventId = 4L;
		SeatSelectRequest request = new SeatSelectRequest();
		request.setSeatList(List.of());
		request.setTicketCount(3);

		given(seatService.selectSeat(eq(eventId), any(SeatSelectRequest.class), anyLong()))
			.willThrow(new ConflictException("[selectSeats] 예매 가능한 좌석 수가 부족합니다."));

		// when & then
		MvcResult result = mockMvc.perform(post("/api/v1/event/{eventId}/seats", eventId)
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("409-CONFLICT"))
			.andExpect(jsonPath("$.msg").value("[selectSeats] 예매 가능한 좌석 수가 부족합니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("좌석 선택 실패 - 이미 예매된 좌석 선택 시 409 반환")
	void selectSeats_alreadyReservedSeat_fail() throws Exception {
		// given
		Long eventId = 5L;
		SeatSelectRequest request = new SeatSelectRequest();
		request.setSeatList(List.of(10L));
		request.setTicketCount(1);

		given(seatService.selectSeat(eq(eventId), any(SeatSelectRequest.class), anyLong()))
			.willThrow(new ConflictException("[selectSeats] 이미 예매된 좌석입니다. seatId = 10"));

		// when & then
		MvcResult result = mockMvc.perform(post("/api/v1/event/{eventId}/seats", eventId)
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("409-CONFLICT"))
			.andExpect(jsonPath("$.msg").value("[selectSeats] 이미 예매된 좌석입니다. seatId = 10"))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("좌석 취소 성공 - 200 반환")
	void cancelSeats_success() throws Exception {
		// given
		SeatCancelRequest request = new SeatCancelRequest();
		request.setSeatList(List.of(101L));

		Long eventId = 1L;

		willDoNothing().given(seatService).cancelSeat(eq(eventId), any(SeatCancelRequest.class), anyLong());
		willDoNothing().given(entryTokenValidator).validate(anyLong(), anyString());

		// when & then
		MvcResult result = mockMvc.perform(delete("/api/v1/event/{eventId}/seats", eventId)
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.msg").value("좌석 취소 성공"))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("좌석 취소 실패 - 존재하지 않는 좌석의 경우 404 반환")
	void cancelSeats_fail_seatNotFound() throws Exception {
		// given
		SeatCancelRequest request = new SeatCancelRequest();
		request.setSeatList(List.of(999L));
		Long eventId = 1L;

		willThrow(new IllegalArgumentException("[cancelSeat] 좌석을 찾을 수 없습니다. seatId: 999"))
			.given(seatService).cancelSeat(eq(eventId), any(SeatCancelRequest.class), anyLong());
		willDoNothing().given(entryTokenValidator).validate(anyLong(), anyString());

		// when & then
		MvcResult result = mockMvc.perform(delete("/api/v1/event/{eventId}/seats", eventId)
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404-NOT_FOUND"))
			.andExpect(jsonPath("$.msg").value("[cancelSeat] 좌석을 찾을 수 없습니다. seatId: 999"))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("좌석 취소 실패 - Redis 락 해제 실패 시 400 반환")
	void cancelSeats_fail_unlockFailed() throws Exception {
		// given
		SeatCancelRequest request = new SeatCancelRequest();
		request.setSeatList(List.of(101L));
		Long eventId = 1L;

		willThrow(new BadRequestException("[cancelSeat] 좌석 락을 해제할 수 없습니다."))
			.given(seatService).cancelSeat(eq(eventId), any(SeatCancelRequest.class), anyLong());
		willDoNothing().given(entryTokenValidator).validate(anyLong(), anyString());

		// when & then
		MvcResult result = mockMvc.perform(delete("/api/v1/event/{eventId}/seats", eventId)
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.msg").value("[cancelSeat] 좌석 락을 해제할 수 없습니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}
}
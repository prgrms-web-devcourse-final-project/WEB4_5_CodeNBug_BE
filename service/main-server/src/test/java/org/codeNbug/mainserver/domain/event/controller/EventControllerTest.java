package org.codeNbug.mainserver.domain.event.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.EventInfoResponse;
import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.CostRange;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.service.CommonEventService;
import org.codeNbug.mainserver.domain.event.service.EventViewCountUpdateScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@WebMvcTest(controllers = EventController.class)
@ComponentScan(
		excludeFilters = @ComponentScan.Filter(
				type = FilterType.ASSIGNABLE_TYPE,
				classes = org.codeNbug.mainserver.global.config.BatchConfig.class
		)
)
class EventControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CommonEventService commonEventService;
	@MockitoBean
	private EventViewCountUpdateScheduler eventViewCountUpdateScheduler;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("이벤트 조회 테스트 - 단순 조회")
	@WithMockUser
	public void getEventsTest() throws Exception {

		Pageable pageable = PageRequest.of(0, 10);

		Mockito.when(commonEventService.getEvents(null, null, pageable))
			.thenReturn(new PageImpl<>(List.of(new EventListResponse()), Pageable.ofSize(10), 1));

		mockMvc.perform(post("/api/v1/events")
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("event list 조회 성공."))
			.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	@DisplayName("이벤트 조회 테스트 - 잘못된 costRange 필터 입력")
	@WithMockUser
	public void getEventsWithFilterTest() throws Exception {
		Pageable pageable = PageRequest.of(0, 10);
		EventListFilter filter = new EventListFilter.Builder()
			.costRange(new CostRange(-1, -1))
			.build();
		Mockito.when(commonEventService.getEvents(null, filter, pageable))
			.thenReturn(new PageImpl<>(List.of(new EventListResponse()), Pageable.ofSize(10), 1));

		mockMvc.perform(post("/api/v1/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(filter))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400-BAD_REQUEST"))
			.andExpect(jsonPath("$.msg").value("데이터 형식이 잘못되었습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	@DisplayName("이벤트 조회 테스트 - startDate가 endDate보다 앞에 있는 경우 실패")
	@WithMockUser
	public void getEventsWithInvalidDateRangeTest() throws Exception {
		Pageable pageable = PageRequest.of(0, 10);
		LocalDateTime startDate = LocalDateTime.now().plusDays(2);
		LocalDateTime endDate = LocalDateTime.now();

		Mockito.when(commonEventService.getEvents(Mockito.any(), Mockito.any(), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(new EventListResponse()), Pageable.ofSize(10), 1));

		mockMvc.perform(post("/api/v1/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"startDate": "%s",
						"endDate": "%s"
					}
					""".formatted(startDate.toString(), endDate.toString()))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400-BAD_REQUEST"))
			.andExpect(jsonPath("$.msg").value("데이터 형식이 잘못되었습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	@DisplayName("카테고리 리스트 조회")
	@WithMockUser
	public void getCategoryListTest() throws Exception {
		Mockito.when(commonEventService.getEventCategories())
			.thenReturn(List.of(EventCategoryEnum.values()));

		mockMvc.perform(get("/api/v1/events/categories")
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("Event categories retrieved successfully"))
			.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	@DisplayName("예매 가능 좌석수 조회")
	@WithMockUser
	public void getAvailableSeatCountTest() throws Exception {
		Long eventId = 1L;
		Integer availableSeats = 100;

		Mockito.when(commonEventService.getAvailableSeatCount(eventId))
			.thenReturn(availableSeats);

		mockMvc.perform(get("/api/v1/events/" + eventId + "/seats")
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("가능한 좌석수 조회 성공"))
			.andExpect(jsonPath("$.data").value(availableSeats));
	}

	@Test
	@DisplayName("예매 가능 좌석수 조회 - 존재하지 않는 이벤트 id로 조회")
	@WithMockUser
	public void getAvailableSeatCountWithInvalidEventIdTest() throws Exception {
		Long eventId = 999L;

		Mockito.when(commonEventService.getAvailableSeatCount(eventId))
			.thenThrow(new IllegalArgumentException("해당 id의 event는 없습니다."));

		mockMvc.perform(get("/api/v1/events/" + eventId + "/seats")
				.with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404-NOT_FOUND"))
			.andExpect(jsonPath("$.msg").value("해당 id의 event는 없습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	@DisplayName("이벤트 단건 조회")
	@WithMockUser
	public void getEventTest() throws Exception {
		Long eventId = 1L;
		EventInfoResponse eventInfoResponse = new EventInfoResponse();

		Mockito.when(commonEventService.getEvent(eventId))
			.thenReturn(eventInfoResponse);

		mockMvc.perform(get("/api/v1/events/" + eventId)
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("event 단건 조회 성공."))
			.andExpect(jsonPath("$.data").exists());
	}

	@Test
	@DisplayName("이벤트 단건 조회 - 존재하지 않는 이벤트 id로 조회")
	@WithMockUser
	public void getEventWithInvalidEventIdTest() throws Exception {
		Long eventId = 999L;

		Mockito.when(commonEventService.getEvent(eventId))
			.thenThrow(new IllegalArgumentException("해당 id의 event는 없습니다."));

		mockMvc.perform(get("/api/v1/events/" + eventId)
				.with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404-NOT_FOUND"))
			.andExpect(jsonPath("$.msg").value("해당 id의 event는 없습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

}
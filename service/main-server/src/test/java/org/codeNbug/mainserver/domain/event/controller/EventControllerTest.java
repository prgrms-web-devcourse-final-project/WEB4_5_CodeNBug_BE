package org.codeNbug.mainserver.domain.event.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.CostRange;
import org.codeNbug.mainserver.domain.event.service.CommonEventService;
import org.codeNbug.mainserver.domain.event.service.EventViewCountUpdateScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
public class EventControllerTest {

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
			.thenReturn(List.of(new EventListResponse()));

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
			.thenReturn(List.of(new EventListResponse()));

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
			.thenReturn(List.of(new EventListResponse()));

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
	

}
package org.codeNbug.mainserver.domain.event.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

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
}

package org.codeNbug.mainserver.domain.event.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.codeNbug.mainserver.domain.event.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.util.TestUtil;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	private ObjectMapper objectMapper = new ObjectMapper()
		.registerModule(new JavaTimeModule())
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	@BeforeEach
	void setUp() {
		// Set up MockMvc

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
	@DisplayName("이벤트 카테고리 조회 테스트")
	void getEventCategories() throws Exception {
		// when
		ResultActions result = mockMvc.perform(get("/api/v1/events/categories")
			.contentType(MediaType.APPLICATION_JSON));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("Event categories retrieved successfully"))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(3))
			.andExpect(jsonPath("$.data[0].name").value("Concert"))
			.andExpect(jsonPath("$.data[1].name").value("Sports"))
			.andExpect(jsonPath("$.data[2].name").value("Theater"));
	}

	@Test
	@DisplayName("이벤트 좌석 수 조회 테스트")
	void getAvailableSeatCount() throws Exception {
		// when
		ResultActions result = mockMvc.perform(get("/api/v1/events/1/seats")
			.contentType(MediaType.APPLICATION_JSON));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("가능한 좌석수 조회 성공"))
			.andExpect(jsonPath("$.data").value(100));
	}

	@Test
	@DisplayName("이벤트 상세 조회 테스트")
	void getEvent() throws Exception {
		// when
		ResultActions result = mockMvc.perform(get("/api/v1/events/1")
			.contentType(MediaType.APPLICATION_JSON));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("event 단건 조회 성공."))
			.andExpect(jsonPath("$.data").exists());
	}

	@Test
	@DisplayName("이벤트 조회수 업데이트 테스트")
	void updateViewCount() throws Exception {
		// when
		ResultActions result = mockMvc.perform(patch("/api/v1/events/view")
			.contentType(MediaType.APPLICATION_JSON));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("업데이트 성공"));
	}
}

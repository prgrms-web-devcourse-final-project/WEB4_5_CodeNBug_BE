package org.codeNbug.mainserver.domain.event.controller;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.service.CommonEventService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EventController {

	private final CommonEventService commonEventService;

	public EventController(CommonEventService commonEventService) {
		this.commonEventService = commonEventService;
	}

	public ResponseEntity<RsData<List<EventListResponse>>> getEvents(@RequestParam(name = "keyword") String keyword,
		@ModelAttribute EventListFilter filter) {

		List<EventListResponse> eventList = commonEventService.getEvents(keyword, filter);

		return ResponseEntity.ok(RsData.success("event list 조회 성공.", eventList));
	}
}

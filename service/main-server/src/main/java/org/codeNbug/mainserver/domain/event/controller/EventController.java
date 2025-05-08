package org.codeNbug.mainserver.domain.event.controller;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.EventInfoResponse;
import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.service.CommonEventService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@PostMapping("/events")
	public ResponseEntity<RsData<List<EventListResponse>>> getEvents(
		@RequestParam(name = "keyword", required = false) String keyword,
		@RequestBody EventListFilter filter) {

		List<EventListResponse> eventList = commonEventService.getEvents(keyword, filter);

		return ResponseEntity.ok(RsData.success("event list 조회 성공.", eventList));
	}

	@GetMapping("/events/{id}")
	public ResponseEntity<RsData<EventInfoResponse>> getEvent(@PathVariable(name = "id") Long id) {
		EventInfoResponse event = commonEventService.getEvent(id);

		return ResponseEntity.ok(RsData.success("event 단건 조회 성공.", event));
	}
}

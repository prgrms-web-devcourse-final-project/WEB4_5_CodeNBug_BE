package org.codeNbug.mainserver.domain.event.controller;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.EventInfoResponse;
import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.EventType;
import org.codeNbug.mainserver.domain.event.service.CommonEventService;
import org.codeNbug.mainserver.domain.event.service.EventViewCountUpdateScheduler;
import org.codeNbug.mainserver.global.dto.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EventController {

	private final CommonEventService commonEventService;
	private final EventViewCountUpdateScheduler eventViewCountUpdateScheduler;

	public EventController(CommonEventService commonEventService,
		EventViewCountUpdateScheduler eventViewCountUpdateScheduler) {
		this.commonEventService = commonEventService;
		this.eventViewCountUpdateScheduler = eventViewCountUpdateScheduler;
	}

	@PostMapping("/events")
	public ResponseEntity<RsData<List<EventListResponse>>> getEvents(
		@RequestParam(name = "keyword", required = false) String keyword,
		@RequestBody(required = false) EventListFilter filter) {

		List<EventListResponse> eventList = commonEventService.getEvents(keyword, filter);

		return ResponseEntity.ok(RsData.success("event list 조회 성공.", eventList));
	}

	@GetMapping("/events/categories")
	public ResponseEntity<RsData<List<EventType>>> getEventCategories() {
		List<EventType> eventTypes = commonEventService.getEventCategories();
		return ResponseEntity.ok(RsData.success("Event categories retrieved successfully", eventTypes));
	}

	@GetMapping("/events/{id}/seats")
	public ResponseEntity<RsData<Integer>> getAvailableSeatCount(
		@PathVariable(name = "id") Long eventId
	) {
		Integer seatCount = commonEventService.getAvailableSeatCount(eventId);

		return ResponseEntity.ok(RsData.success("가능한 좌석수 조회 성공", seatCount));
	}

	@GetMapping("/events/{id}")
	public ResponseEntity<RsData<EventInfoResponse>> getEvent(@PathVariable(name = "id") Long id) {
		EventInfoResponse event = commonEventService.getEvent(id);

		return ResponseEntity.ok(RsData.success("event 단건 조회 성공.", event));
	}

	@PatchMapping("/events/view")
	public ResponseEntity<RsData<Void>> updateViewCount() {
		eventViewCountUpdateScheduler.updateViewCount();
		return ResponseEntity.ok(RsData.success("업데이트 성공"));
	}
}

package org.codeNbug.mainserver.domain.event.controller;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.EventInfoResponse;
import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.service.CommonEventService;
import org.codeNbug.mainserver.domain.event.service.EventViewCountUpdateScheduler;
import org.codeNbug.mainserver.global.dto.RsData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

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
	public ResponseEntity<RsData<Page<EventListResponse>>> getEvents(
		@RequestParam(name = "keyword", required = false) String keyword,
		@Valid @RequestBody(required = false) EventListFilter filter,
		Pageable pageable) {
		PageRequest page = PageRequest.of(pageable.getPageNumber() - 1, pageable.getPageSize());
		Page<EventListResponse> eventList = commonEventService.getEvents(keyword, filter, page);

		return ResponseEntity.ok(RsData.success("event list 조회 성공.", eventList));
	}

	@GetMapping("/events/categories")
	public ResponseEntity<RsData<List<EventCategoryEnum>>> getEventCategories() {
		List<EventCategoryEnum> categories = commonEventService.getEventCategories();
		return ResponseEntity.ok(RsData.success("Event categories retrieved successfully", categories));
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

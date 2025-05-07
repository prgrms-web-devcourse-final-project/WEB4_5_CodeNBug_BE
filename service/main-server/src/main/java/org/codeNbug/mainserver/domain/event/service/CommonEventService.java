package org.codeNbug.mainserver.domain.event.service;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.CommonEventRepository;
import org.springframework.stereotype.Service;

@Service
public class CommonEventService {

	private final CommonEventRepository commonEventRepository;

	public CommonEventService(CommonEventRepository commonEventRepository) {
		this.commonEventRepository = commonEventRepository;
	}

	public List<EventListResponse> getEvents(String keyword, EventListFilter filter) {
		if (keyword == null || keyword.isEmpty()) {
			return getEventsOnlyFilters(filter);
		} else if (filter == null || !filter.canFiltered()) {
			return getEventsOnlyKeyword(keyword);
		} else {
			return getEventsWithFilterAndKeyword(keyword, filter);
		}
	}

	private List<EventListResponse> getEventsWithFilterAndKeyword(String keyword, EventListFilter filter) {
		return commonEventRepository.findAllByFilterAndKeyword(keyword, filter)
			.stream().map(event -> new EventListResponse(event)).toList();

	}

	private List<EventListResponse> getEventsOnlyKeyword(String keyword) {
		return commonEventRepository.findAllByKeyword(keyword)
			.stream().map(event -> new EventListResponse(event)).toList();

	}

	private List<EventListResponse> getEventsOnlyFilters(EventListFilter filter) {
		return commonEventRepository.findAllByFilter(filter)
			.stream().map(event -> new EventListResponse(event)).toList();
	}
}

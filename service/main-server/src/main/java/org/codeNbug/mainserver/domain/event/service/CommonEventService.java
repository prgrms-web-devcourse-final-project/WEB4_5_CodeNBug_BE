package org.codeNbug.mainserver.domain.event.service;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.EventInfoResponse;
import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.CommonEventRepository;
import org.codeNbug.mainserver.domain.event.repository.JpaCommonEventRepository;
import org.springframework.stereotype.Service;

@Service
public class CommonEventService {

	private final CommonEventRepository commonEventRepository;
	private final JpaCommonEventRepository jpaCommonEventRepository;

	public CommonEventService(CommonEventRepository commonEventRepository,
		JpaCommonEventRepository jpaCommonEventRepository) {
		this.commonEventRepository = commonEventRepository;
		this.jpaCommonEventRepository = jpaCommonEventRepository;
	}

	public List<EventListResponse> getEvents(String keyword, EventListFilter filter) {
		if (keyword == null || keyword.isEmpty()) {
			return getEventsOnlyFilters(filter);
		} else if (filter == null || !filter.canFiltered()) {
			return getEventsOnlyKeyword(keyword);
		} else if (filter.canFiltered() && keyword != null && !keyword.isEmpty()) {
			return getEventsWithFilterAndKeyword(keyword, filter);
		} else {
			return jpaCommonEventRepository.findByIsDeletedFalse()
				.stream().map(event -> new EventListResponse(event)).toList();
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

	public EventInfoResponse getEvent(Long id) {
		return new EventInfoResponse(jpaCommonEventRepository.findByEventIdAndIsDeletedFalse(id)
			.orElseThrow(() -> new IllegalArgumentException("해당 id의 event는 없습니다.")));
	}
}

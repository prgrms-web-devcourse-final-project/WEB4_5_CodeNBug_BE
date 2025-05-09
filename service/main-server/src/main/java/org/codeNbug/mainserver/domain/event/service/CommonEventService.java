package org.codeNbug.mainserver.domain.event.service;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.EventInfoResponse;
import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.CommonEventRepository;
import org.codeNbug.mainserver.domain.event.entity.EventType;
import org.codeNbug.mainserver.domain.event.repository.JpaCommonEventRepository;
import org.codeNbug.mainserver.domain.manager.repository.EventTypeRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CommonEventService {

	private final CommonEventRepository commonEventRepository;
	private final JpaCommonEventRepository jpaCommonEventRepository;
	private final EventTypeRepository eventTypeRepository;
	private final RedisTemplate<String, Object> redisTemplate;

	public CommonEventService(CommonEventRepository commonEventRepository,
		JpaCommonEventRepository jpaCommonEventRepository, EventTypeRepository eventTypeRepository,
		RedisTemplate<String, Object> redisTemplate) {
		this.commonEventRepository = commonEventRepository;
		this.eventTypeRepository = eventTypeRepository;
		this.jpaCommonEventRepository = jpaCommonEventRepository;
		this.redisTemplate = redisTemplate;
	}

	public List<EventListResponse> getEvents(String keyword, EventListFilter filter) {
		if ((keyword == null || keyword.isEmpty()) && (filter == null || !filter.canFiltered())) {
			return jpaCommonEventRepository.findByIsDeletedFalse()
				.stream().map(event -> new EventListResponse(event)).toList();
		}
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

	public List<EventType> getEventCategories() {
		// Directly fetch all EventType objects
		return eventTypeRepository.findAll();
	}

	public Integer getAvailableSeatCount(Long id) {
		Integer count = commonEventRepository.countAvailableSeat(id);
		return count;
	}

	public EventInfoResponse getEvent(Long id) {
		Integer incrementedViewCount = redisTemplate.opsForValue().increment("viewCount:" + id, 1).intValue();

		EventInfoResponse eventInfoResponse = new EventInfoResponse(
			jpaCommonEventRepository.findByEventIdAndIsDeletedFalse(id)
				.orElseThrow(() -> new IllegalArgumentException("해당 id의 event는 없습니다.")), incrementedViewCount);
		return eventInfoResponse;
	}
}

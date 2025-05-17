package org.codeNbug.mainserver.domain.event.service;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.EventInfoResponse;
import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.CommonEventRepository;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.repository.JpaCommonEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CommonEventService {

	private final CommonEventRepository commonEventRepository;
	private final JpaCommonEventRepository jpaCommonEventRepository;
	private final RedisTemplate<String, Object> redisTemplate;

	public CommonEventService(CommonEventRepository commonEventRepository,
		JpaCommonEventRepository jpaCommonEventRepository,
		RedisTemplate<String, Object> redisTemplate) {
		this.commonEventRepository = commonEventRepository;
		this.jpaCommonEventRepository = jpaCommonEventRepository;
		this.redisTemplate = redisTemplate;
	}

	public Page<EventListResponse> getEvents(String keyword, EventListFilter filter, Pageable pageable) {
		if ((keyword == null || keyword.isEmpty()) && (filter == null || !filter.canFiltered())) {
			return commonEventRepository.findByIsDeletedFalse(pageable)
				.map(tuple -> new EventListResponse(tuple.get(0, Event.class), tuple.get(1, Integer.class),
					tuple.get(2, Integer.class)));
		}
		if (keyword == null || keyword.isEmpty()) {
			return getEventsOnlyFilters(filter, pageable);
		} else if (filter == null || !filter.canFiltered()) {
			return getEventsOnlyKeyword(keyword, pageable);
		} else {
			return getEventsWithFilterAndKeyword(keyword, filter, pageable);
		}
	}

	private Page<EventListResponse> getEventsWithFilterAndKeyword(String keyword, EventListFilter filter,
		Pageable pageable) {
		return commonEventRepository.findAllByFilterAndKeyword(keyword, filter, pageable)
			.map(event -> new EventListResponse(event.get(0, Event.class), event.get(1, Integer.class),
				event.get(2, Integer.class)));

	}

	private Page<EventListResponse> getEventsOnlyKeyword(String keyword, Pageable pageable) {
		return commonEventRepository.findAllByKeyword(keyword, pageable)
			.map(event -> new EventListResponse(event.get(0, Event.class), event.get(1, Integer.class),
				event.get(2, Integer.class)));

	}

	private Page<EventListResponse> getEventsOnlyFilters(EventListFilter filter, Pageable pageable) {
		return commonEventRepository.findAllByFilter(filter, pageable)
			.map(event -> new EventListResponse(event.get(0, Event.class), event.get(1, Integer.class),
				event.get(2, Integer.class)));
	}

	public List<EventCategoryEnum> getEventCategories() {
		return List.of(EventCategoryEnum.values());
	}

	public Integer getAvailableSeatCount(Long id) {
		Integer count = commonEventRepository.countAvailableSeat(id);
		if (count == null)
			throw new IllegalArgumentException("해당 id의 event는 없습니다.");
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

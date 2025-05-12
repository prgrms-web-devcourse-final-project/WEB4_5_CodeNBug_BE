package org.codeNbug.mainserver.domain.event.service;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.EventInfoResponse;
import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.CommonEventRepository;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.repository.JpaCommonEventRepository;
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

	public List<EventListResponse> getEvents(String keyword, EventListFilter filter, Pageable pageable) {
		if ((keyword == null || keyword.isEmpty()) && (filter == null || !filter.canFiltered())) {
			return jpaCommonEventRepository.findByIsDeletedFalse(pageable)
				.stream()
				.map(event -> new EventListResponse(event))
				.toList();
		}
		if (keyword == null || keyword.isEmpty()) {
			return getEventsOnlyFilters(filter, pageable);
		} else if (filter == null || !filter.canFiltered()) {
			return getEventsOnlyKeyword(keyword, pageable);
		} else {
			return getEventsWithFilterAndKeyword(keyword, filter, pageable);
		}
	}

	private List<EventListResponse> getEventsWithFilterAndKeyword(String keyword, EventListFilter filter,
		Pageable pageable) {
		return commonEventRepository.findAllByFilterAndKeyword(keyword, filter, pageable)
			.stream()
			.map(event -> new EventListResponse(event))
			.toList();

	}

	private List<EventListResponse> getEventsOnlyKeyword(String keyword, Pageable pageable) {
		return commonEventRepository.findAllByKeyword(keyword, pageable)
			.stream()
			.map(event -> new EventListResponse(event))
			.toList();

	}

	private List<EventListResponse> getEventsOnlyFilters(EventListFilter filter, Pageable pageable) {
		return commonEventRepository.findAllByFilter(filter, pageable)
			.stream()
			.map(event -> new EventListResponse(event))
			.toList();
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

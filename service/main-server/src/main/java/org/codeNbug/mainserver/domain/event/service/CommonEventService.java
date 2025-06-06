package org.codeNbug.mainserver.domain.event.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
		return commonEventRepository.findAllByFilterAndKeyword(keyword, filter, pageable);
	}

	private Page<EventListResponse> getEventsOnlyKeyword(String keyword, Pageable pageable) {
		return commonEventRepository.findAllByKeyword(keyword, pageable);
	}

	private Page<EventListResponse> getEventsOnlyFilters(EventListFilter filter, Pageable pageable) {
		return commonEventRepository.findAllByFilter(filter, pageable);
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
		Event event = jpaCommonEventRepository.findByEventIdAndIsDeletedFalse(id)
			.orElseThrow(() -> new IllegalArgumentException("해당 id의 event는 없습니다."));

		if (redisTemplate.opsForZSet().score("viewCount:top", "event:" + id) == null) {
			redisTemplate.opsForZSet().add("viewCount:top", "event:" + id, event.getViewCount());
		}
		Integer incrementedViewCount = redisTemplate.opsForZSet()
			.incrementScore("viewCount:top", "event:" + id, 1D)
			.intValue();

		EventInfoResponse eventInfoResponse = new EventInfoResponse(
			event, incrementedViewCount);
		return eventInfoResponse;
	}

	public List<EventListResponse> getRecommends(Long count) {
		// redis cache에서 조회수 상위 10개의 id 가져오기
		Map<Long, Integer> viewCountCache = new LinkedHashMap<>();
		List<Long> idList = redisTemplate.opsForZSet().reverseRangeWithScores("viewCount:top", 0, count - 1)
			.stream().map(item -> {
				viewCountCache.put(Long.parseLong(item.getValue().toString().split(":")[1]),
					item.getScore().intValue());
				return Long.parseLong(item.getValue().toString().split(":")[1]);
			}).toList();

		// id를 기준으로 조회
		return jpaCommonEventRepository.findAllById(idList)
			.stream().map(item -> {
				EventListResponse resp = new EventListResponse(item, item.getMinPrice(), item.getMaxPrice());
				resp.setViewCount(viewCountCache.get(item.getEventId()));
				return resp;
			}).sorted((a, b) -> b.getViewCount() - a.getViewCount())
			.toList();
	}
}

package org.codeNbug.mainserver.domain.event.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.CommonEventRepository;
import org.codeNbug.mainserver.domain.event.entity.CostRange;
import org.codeNbug.mainserver.domain.event.repository.JpaCommonEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CommonEventServiceTest {

	@Mock
	private CommonEventRepository commonEventRepository;
	@Mock
	private JpaCommonEventRepository jpaCommonEventRepository;
	@Mock
	private RedisTemplate<String, Object> redisTemplate;
	@Mock
	private ValueOperations<String, Object> valueOperations;
	@InjectMocks
	private CommonEventService commonEventService;

	@Test
	@DisplayName("filter와 keyword가 모두 비어있을 때 jpaCommonEventRepository.findByIsDeletedFalse가 호출되어야 한다.")
	void getEvents() {
		// given
		String keyword = null;
		EventListFilter filter = null;
		PageRequest pageable = PageRequest.of(0, 10);

		when(jpaCommonEventRepository.findByIsDeletedFalse(pageable))
			.thenReturn(List.of());

		// when
		List<EventListResponse> result = commonEventService.getEvents(keyword, filter, pageable);

		// then
		verify(jpaCommonEventRepository).findByIsDeletedFalse(pageable);
		assertEquals(0, result.size());
	}

	@Test
	@DisplayName("filter만 비어있지 않을 경우 jpaCommonEventRepository.getEventsOnlyFilters가 호출되어야 한다.")
	void getEventsWithOnlyFilter() {
		// given
		String keyword = null;
		EventListFilter filter = new EventListFilter.Builder().costRange(new CostRange(0, 1000)).build();
		PageRequest pageable = PageRequest.of(0, 10);

		when(commonEventRepository.findAllByFilter(eq(filter), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		// when
		List<EventListResponse> result = commonEventService.getEvents(null, filter, pageable);

		// then
		verify(commonEventRepository).findAllByFilter(eq(filter), eq(pageable));
		org.assertj.core.api.Assertions.assertThat(result).isNotNull();
	}

	@Test
	@DisplayName("keyword만 비어있지 않을 경우 getEventsOnlyKeyword가 호출되어야 함")
	void getEventsWithOnlyKeyword() {
		// given
		String keyword = "test";
		EventListFilter filter = null;
		PageRequest pageable = PageRequest.of(0, 10);

		when(commonEventRepository.findAllByKeyword(eq(keyword), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		// when
		List<EventListResponse> result = commonEventService.getEvents(keyword, filter, pageable);

		// then
		verify(commonEventRepository).findAllByKeyword(eq(keyword), eq(pageable));
		org.assertj.core.api.Assertions.assertThat(result).isNotNull();
	}

	@Test
	@DisplayName("keyword, filter가 모두 비어있지 않을 경우 getEventsWithFilterAndKeyword로 분기해야 함")
	void getEventsWithFilterAndKeyword() {
		// given
		String keyword = "test";
		EventListFilter filter = new EventListFilter.Builder().costRange(new CostRange(0, 1000)).build();
		PageRequest pageable = PageRequest.of(0, 10);

		when(commonEventRepository.findAllByFilterAndKeyword(eq(keyword), eq(filter), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		// when
		List<EventListResponse> result = commonEventService.getEvents(keyword, filter, pageable);

		// then
		verify(commonEventRepository).findAllByFilterAndKeyword(eq(keyword), eq(filter), eq(pageable));
		org.assertj.core.api.Assertions.assertThat(result).isNotNull();
	}

	@Test
	@DisplayName("가능한 좌석수 조회 - repository에서 조회한 값이 null일경우 exception을 발생시켜야 한다")
	void getAvailableSeatCount() {
		// given
		Long eventId = 1L;
		when(commonEventRepository.countAvailableSeat(eventId)).thenReturn(null);

		// when & then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> commonEventService.getAvailableSeatCount(eventId));
		assertEquals("해당 id의 event는 없습니다.", exception.getMessage());
		verify(commonEventRepository).countAvailableSeat(eventId);
	}

	@Test
	@DisplayName("이벤트 단건 조회 - 존재하지 않는 이벤트 조회시 exception을 발생시켜야 한다")
	void getEvent() {
		// given
		Long eventId = 1L;

		when(jpaCommonEventRepository.findByEventIdAndIsDeletedFalse(eventId))
			.thenReturn(java.util.Optional.empty());
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		when(valueOperations.increment(eq("viewCount:" + eventId), eq(1L)))
			.thenReturn(1L);

		// when & then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> commonEventService.getEvent(eventId));
		assertEquals("해당 id의 event는 없습니다.", exception.getMessage());
		verify(jpaCommonEventRepository).findByEventIdAndIsDeletedFalse(eventId);
		verify(redisTemplate.opsForValue()).increment(eq("viewCount:" + eventId), eq(1L));
	}
}
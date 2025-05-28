package org.codeNbug.mainserver.domain.manager.service;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.dto.ManagerEventListResponse;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codenbug.user.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * 1. searchEventList 테스트
 *  - 이벤트가 존재할 때
 *  - 이벤트가 없을 때
 *
 * 2. searchEvent 테스트
 *  - 정상 케이스
 *  findByIdWithSeats() → Event 반환
 *  validateManagerAuthority() 호출
 *  seatGradeRepository.findByEvent() 호출
 *  layout json 파싱 정상
 *
 *  - 예외 케이스
 *  findByIdWithSeats() → empty → IllegalArgumentException
 *  validateManagerAuthority() → exception 발생 시 테스트
 */
@ExtendWith(MockitoExtension.class)
class ManagerEventSearchServiceTest {
    @Mock
    private ManagerEventRepository managerEventRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventDomainService eventDomainService;
    @Mock
    private SeatGradeRepository seatGradeRepository;

    @InjectMocks
    private ManagerEventSearchService managerEventSearchService;

    @Test
    @DisplayName("매니저가 담담하는 이벤트 목록 조회 성공")
    void searchEventList_success() {
        // given
        User currentUser = User.builder()
                .userId(1L)
                .email("manager@example.com")
                .build();
        LocalDateTime eventStart = LocalDateTime.now();
        LocalDateTime eventEnd = LocalDateTime.now().plusDays(1);
        LocalDateTime bookingStart = LocalDateTime.now().minusDays(7);
        LocalDateTime bookingEnd = LocalDateTime.now().plusHours(12);

        Event event1 = new Event(
                EventCategoryEnum.CONCERT,
                EventInformation.builder()
                        .title("콘서트 1")
                        .description("설명 없음")
                        .ageLimit(0)
                        .restrictions("")
                        .location("서울")
                        .hallName("올림픽홀")
                        .eventStart(eventStart)
                        .eventEnd(eventEnd)
                        .seatCount(100)
                        .thumbnailUrl("http://image1")
                        .build(),
                bookingStart,
                bookingEnd,
                0,
                null,
                null,
                EventStatusEnum.OPEN,
                true,
                false,
                null
        );

        Event event2 = new Event(
                EventCategoryEnum.SPORTS,
                EventInformation.builder()
                        .title("스포츠 경기")
                        .description("설명 없음")
                        .ageLimit(0)
                        .restrictions("")
                        .location("부산")
                        .hallName("사직구장")
                        .eventStart(eventStart)
                        .eventEnd(eventEnd)
                        .seatCount(200)
                        .thumbnailUrl("http://image2")
                        .build(),
                bookingStart,
                bookingEnd,
                0,
                null,
                null,
                EventStatusEnum.OPEN,
                true,
                false,
                null
        );

        List<Event> mockEvents = List.of(event1, event2);

        when(managerEventRepository.findEventsByManager(currentUser))
                .thenReturn(mockEvents);

        // when
        List<ManagerEventListResponse> result = managerEventSearchService.searchEventList(currentUser);

        // then
        verify(managerEventRepository).findEventsByManager(currentUser);
        assertThat(result).hasSize(2);

        ManagerEventListResponse first = result.get(0);
        assertThat(first.getEventId()).isEqualTo(event1.getEventId());
        assertThat(first.getTitle()).isEqualTo("콘서트 1");
        assertThat(first.getCategory()).isEqualTo(EventCategoryEnum.CONCERT);
        assertThat(first.getThumbnailUrl()).isEqualTo("http://image1");
        assertThat(first.getStatus()).isEqualTo(EventStatusEnum.OPEN);
        assertThat(first.getIsDeleted()).isFalse();

        // 행사 기간 검증 추가
        assertThat(first.getStartDate()).isEqualTo(eventStart);
        assertThat(first.getEndDate()).isEqualTo(eventEnd);

        // 예매 기간 검증 추가
        assertThat(first.getBookingStart()).isEqualTo(bookingStart);
        assertThat(first.getBookingEnd()).isEqualTo(bookingEnd);

    }

    @Test
    @DisplayName("매니저가 담당하는 이벤트가 없을 경우 빈 리스트 반환")
    void searchEventList_empty() {
        // given
        User currentUser = User.builder()
                .userId(1L)
                .email("manager@example.com")
                .build();

        // Mocking : repository가 빈 리스트 반환하도록 설정
        when(managerEventRepository.findEventsByManager(currentUser))
                .thenReturn(List.of());

        // when : 테스트 대상 메서드 호출
        List<ManagerEventListResponse> result = managerEventSearchService.searchEventList(currentUser);

        // then : 결과 검증
        verify(managerEventRepository).findEventsByManager(currentUser);  // repository 호출 검증
        assertThat(result).isNotNull();                                   // 반환값이 null이 아니어야 함
        assertThat(result).isEmpty();                                     // 반환 리스트가 비어 있어야 함
    }




}
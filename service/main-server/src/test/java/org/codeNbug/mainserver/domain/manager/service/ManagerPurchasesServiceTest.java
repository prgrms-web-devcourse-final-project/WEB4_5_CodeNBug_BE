package org.codeNbug.mainserver.domain.manager.service;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.manager.dto.EventPurchaseResponse;
import org.codeNbug.mainserver.domain.manager.dto.TicketDto;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerPurchasesServiceTest {
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private ManagerEventRepository managerEventRepository;

    @InjectMocks
    private ManagerPurchasesService managerPurchasesService;

    // 정상 조회 테스트
    @DisplayName("이벤트 구매 내역 조회에 성공")
    @Test
    void 이벤트_구매_내역_조회_성공() {
        // given
        Long eventId = 1L;
        Long managerId = 10L;

        User manager = User.builder()
                .userId(managerId)
                .email("manager@test.com")
                .build();

        Event event = new Event();

        ReflectionTestUtils.setField(event, "eventId", eventId);

        List<TicketDto> dummyTickets = List.of(
                new TicketDto(
                        100L,        // purchaseId
                        200L,        // userId
                        "user1",     // userName
                        "user1@test.com", // userEmail
                        "010-1234-5678",  // phoneNum
                        PaymentStatusEnum.DONE,
                        LocalDateTime.now(), // purchaseAt
                        5000,         // amount
                        300L        // ticket_id
                ),
                new TicketDto(
                        101L,        // purchaseId
                        201L,        // userId
                        "user2",     // userName
                        "user2@test.com", // userEmail
                        "010-2234-5678",  // phoneNum
                        PaymentStatusEnum.DONE,
                        LocalDateTime.now(), // purchaseAt
                        15000,         // amount
                        301L        // ticket_id
                )
        );

        // when - mock 설정
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(ticketRepository.findTicketPurchasesByEventId(eventId)).thenReturn(dummyTickets);

        // then
        List<EventPurchaseResponse> result = managerPurchasesService.getEventPurchaseList(eventId, manager);

        assertNotNull(result);
        assertEquals(2, result.size());

        EventPurchaseResponse response = result.get(0);
        assertEquals(100L, response.getPurchaseId());
        assertEquals(200L, response.getUserId());
        assertEquals("user1", response.getUserName());
        assertEquals("user1@test.com", response.getUserEmail());
        assertEquals("010-1234-5678", response.getPhoneNum());
        assertEquals(PaymentStatusEnum.DONE, response.getPayment_status());
        assertEquals(List.of(300L), response.getTicket_id());    // 구매 티켓 1개
        assertEquals(5000, response.getAmount());

        response = result.get(1);
        assertEquals(101L, response.getPurchaseId());
        assertEquals(201L, response.getUserId());
        assertEquals("user2", response.getUserName());
        assertEquals("user2@test.com", response.getUserEmail());
        assertEquals("010-2234-5678", response.getPhoneNum());
        assertEquals(PaymentStatusEnum.DONE, response.getPayment_status());
        assertEquals(List.of(301L), response.getTicket_id());    // 구매 티켓 1개
        assertEquals(15000, response.getAmount());
    }

    // event 존재 하지 않음 테스트
    // 정상 조회 테스트
    @DisplayName("이벤트가 존재하지 않을 때 조회 오류 테스트")
    @Test
    void 이벤트가_존재하지_않을_때_예외_발생() {
        // given
        Long eventId = 1L;
        Long managerId = 10L;

        User manager = User.builder()
                .userId(managerId)
                .email("manager@test.com")
                .build();

        // when - mock 설정
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // then
        assertThrows(BadRequestException.class, () ->
                managerPurchasesService.getEventPurchaseList(eventId, manager)
        );
    }


    // manager 권한 없음 테스트
    @DisplayName("매니저 권한이 없을 때 조회 오류 테스트")
    @Test
    void 매니저_권한이_없을_때_예외_발생() {
        // given
        Long eventId = 1L;
        Long managerId = 10L;

        User manager = User.builder()
                .userId(managerId)
                .email("manager@test.com")
                .build();

        Event event = new Event();
        ReflectionTestUtils.setField(event, "eventId", eventId);

        // when - mock 설정
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(managerEventRepository.existsByManagerAndEvent(manager, event)).thenReturn(false); //권한 없음

        // then
        assertThrows(AccessDeniedException.class, () ->
                managerPurchasesService.getEventPurchaseList(eventId, manager)
        );
    }

}
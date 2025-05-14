package org.codeNbug.mainserver.domain.manager.service;

import java.util.List;
import java.util.Optional;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.notification.service.NotificationService;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;

@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class EventDeleteServiceTest {

	@Mock
	private EventRepository eventRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private ManagerEventRepository managerEventRepository;
	@Mock
	private NotificationService notificationService;
	@Mock
	private PurchaseRepository purchaseRepository;

	@InjectMocks
	private EventDeleteService eventDeleteService;

	@DisplayName("이벤트 삭제 비즈니스 로직 검증 - 상태 변경 및 중복 삭제 예외")
	@Test
	void deleteEvent_shouldChangeStatusAndPreventDuplicateDelete() throws Exception {
		// given
		Long eventId = 1L;
		Long managerId = 1L;

		Event event = new Event();
		User manager = new User();

		ReflectionTestUtils.setField(event, "eventId", eventId);
		ReflectionTestUtils.setField(manager, "userId", managerId);

		EventInformation eventInformation = new EventInformation();
		ReflectionTestUtils.setField(eventInformation, "title", "테스트 이벤트");
		ReflectionTestUtils.setField(event, "information", eventInformation);

		Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
		Mockito.when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));
		Mockito.when(managerEventRepository.existsByManagerAndEvent(manager, event)).thenReturn(true);
		Mockito.when(purchaseRepository.findAllByEventId(eventId)).thenReturn(List.of());  // 구매자 없음

		// when: 첫 번째 삭제
		eventDeleteService.deleteEvent(eventId, managerId);

		// then: 상태 변경 확인
		Assertions.assertTrue(event.getIsDeleted(), "이벤트가 삭제 상태여야 합니다.");
		Assertions.assertEquals(EventStatusEnum.CANCELLED, event.getStatus(), "이벤트 상태가 CANCELLED여야 합니다.");

		// when & then: 두 번째 삭제 시 예외 발생
		Assertions.assertThrows(IllegalAccessException.class, () -> eventDeleteService.deleteEvent(eventId, managerId));
	}

}
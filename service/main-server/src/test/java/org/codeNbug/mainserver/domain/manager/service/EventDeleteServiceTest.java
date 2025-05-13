package org.codeNbug.mainserver.domain.manager.service;

import java.util.Optional;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
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

	@DisplayName("같은 이벤트를 여러번 삭제할 수 없다.")
	@Test
	void duplicateEventDeleteTest() throws Exception {
		// given
		// 실행에 필요한 빈 모킹
		Long eventId = 1L;
		Long managerId = 1L;
		Event event = new Event();
		User manager = new User();
		ReflectionTestUtils.setField(event, "eventId", eventId);
		ReflectionTestUtils.setField(manager, "userId", managerId);

		EventInformation eventInformation = new EventInformation();
		ReflectionTestUtils.setField(eventInformation, "title", "title");
		ReflectionTestUtils.setField(event, "information", eventInformation);

		Mockito.when(eventRepository.findById(eventId))
			.thenReturn(java.util.Optional.of(event));

		Mockito.when(userRepository.findById(managerId))
			.thenReturn(Optional.of(manager));
		Mockito.when(managerEventRepository.existsByManagerAndEvent(manager, event))
			.thenReturn(true);

		Mockito.when(purchaseRepository.findAllByEventId(eventId))
			.thenReturn(java.util.List.of());

		// 삭제

		eventDeleteService.deleteEvent(eventId, managerId);

		// 두번째 삭제시 exception 발생해야 함

		Assertions.assertThrows(IllegalAccessException.class, () -> eventDeleteService.deleteEvent(eventId, managerId));

	}
}
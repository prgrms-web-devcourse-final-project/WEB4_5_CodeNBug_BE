package org.codeNbug.mainserver.domain.manager.service;

import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagerService {
    private final EventRepository eventRepository;

    public EventRegisterResponse eventRegister(EventRegisterRequest request) {
        return null;
    }
}

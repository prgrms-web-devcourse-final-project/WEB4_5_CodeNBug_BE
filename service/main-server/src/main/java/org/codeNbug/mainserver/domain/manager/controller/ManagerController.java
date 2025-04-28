package org.codeNbug.mainserver.domain.manager.controller;


import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.manager.service.ManagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/events")
public class ManagerController {
    private final ManagerService managerService;

    @PostMapping
    public ResponseEntity<EventRegisterResponse> eventRegister(@RequestBody EventRegisterRequest request) {
        EventRegisterResponse response = managerService.eventRegister(request);
        return null;
    }
}

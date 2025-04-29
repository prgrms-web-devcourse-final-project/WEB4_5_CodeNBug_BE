package org.codeNbug.mainserver.domain.manager.controller;


import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.manager.service.ManagerService;
import org.codeNbug.mainserver.global.dto.RsData;
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

    /**
     * 이벤트 등록 API
     * @param request 이벤트 등록 요청 DTO
     * @return 성공 시 RsData<EventRegisterResponse> 포맷으로 응답
     */
    @PostMapping
    public ResponseEntity<RsData<EventRegisterResponse>> eventRegister(@RequestBody EventRegisterRequest request) {
        EventRegisterResponse response = managerService.registerEvent(request);
        return ResponseEntity.ok(RsData.success("이벤트 등록 성공", response));
    }
}

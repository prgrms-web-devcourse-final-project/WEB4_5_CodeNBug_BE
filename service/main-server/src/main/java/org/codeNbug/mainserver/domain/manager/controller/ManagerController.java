package org.codeNbug.mainserver.domain.manager.controller;


import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.manager.service.EventEditService;
import org.codeNbug.mainserver.domain.manager.service.EventRegisterService;
import org.codeNbug.mainserver.domain.manager.service.EventDeleteService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/events")
public class ManagerController {
    private final EventRegisterService eventRegisterService;
    private final EventEditService eventEditService;
    private final EventDeleteService eventDeleteService;
    /**
     * 이벤트 등록 API
     * @param request 이벤트 등록 요청 DTO
     * @return 성공 시 RsData<EventRegisterResponse> 포맷으로 응답
     */
    @PostMapping
    public ResponseEntity<RsData<EventRegisterResponse>> eventRegister(@RequestBody EventRegisterRequest request) {
        EventRegisterResponse response = eventRegisterService.registerEvent(request);
        return ResponseEntity.ok(new RsData<>(
                "200",
                "이벤트 등록 성공",
                response
        ));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<RsData<EventRegisterResponse>> updateEvent(
            @PathVariable Long eventId,
            @RequestBody EventRegisterRequest request
    ) {
        EventRegisterResponse response = eventEditService.editEvent(eventId, request);
        return ResponseEntity.ok(new RsData<>(
                "200",
                "이벤트 수정 성공",
                response
        ));
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<RsData<Void>> deleteEvent(@PathVariable Long eventId) {
        eventDeleteService.deleteEvent(eventId);
        return ResponseEntity.ok(new RsData<> (
                "200",
                "이벤트 삭제 성공",
                null
        ));
    }

}

package org.codeNbug.mainserver.domain.admin.dto.response;

import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAdminDto {
    private Long eventId;
    private String title;
    private String category;
    private LocalDateTime eventStart;
    private LocalDateTime eventEnd;
    private Integer seatCount; // 총 티켓 수
    private Integer soldTickets; // 판매된 티켓 수
    private String status;
    
    public static EventAdminDto fromEntity(Event event, int soldTickets) {
        EventInformation information = event.getInformation();
        
        return EventAdminDto.builder()
                .eventId(event.getEventId())
                .title(information.getTitle())
                .category(event.getCategory().name())
                .eventStart(information.getEventStart())
                .eventEnd(information.getEventEnd())
                .seatCount(information.getSeatCount())
                .soldTickets(soldTickets)
                .status(event.getStatus().name())
                .build();
    }
} 
package org.codeNbug.mainserver.domain.manager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@FilterDef(name = "activeEventFilter", parameters = @ParamDef(name = "isDeleted", type = Boolean.class))
@Filter(name = "activeEventFilter", condition = "is_deleted = :isDeleted")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @Column(nullable = false)
    private Long typeId;

    @Embedded
    private EventInformation information;

    private LocalDateTime bookingStart;

    private LocalDateTime bookingEnd;

    @Column(columnDefinition = "int default 0")
    private Integer viewCount;


    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime modifiedAt;

    @Enumerated(EnumType.STRING)
    private EventStatusEnum status;

    @Column(columnDefinition = "boolean default true")
    private Boolean seatSelectable;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
}

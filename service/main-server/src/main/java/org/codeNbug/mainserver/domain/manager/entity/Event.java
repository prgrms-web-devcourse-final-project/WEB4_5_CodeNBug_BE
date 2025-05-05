package org.codeNbug.mainserver.domain.manager.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "ManagerEvent")
@Table(name = "event")
@Getter
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
	@Setter
	private Long typeId;

	@Embedded
	@Setter
	private EventInformation information;

	@Setter
	private LocalDateTime bookingStart;

	@Setter
	private LocalDateTime bookingEnd;
	@Setter
	@Column(columnDefinition = "int default 0")
	private Integer viewCount;

	@CreatedDate
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime modifiedAt;
	@Setter
	@Enumerated(EnumType.STRING)
	private EventStatusEnum status;
	@Setter
	@Column(columnDefinition = "boolean default true")
	private Boolean seatSelectable;
	@Setter
	@Column(name = "is_deleted")
	private Boolean isDeleted = false;
}

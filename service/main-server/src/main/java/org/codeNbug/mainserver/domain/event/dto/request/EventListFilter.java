package org.codeNbug.mainserver.domain.event.dto.request;

import java.time.LocalDate;
import java.util.List;

import org.codeNbug.mainserver.domain.event.entity.CostRange;
import org.codeNbug.mainserver.domain.event.entity.Location;
import org.codeNbug.mainserver.domain.manager.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.entity.EventType;

import lombok.Getter;

@Getter
public class EventListFilter {
	private CostRange costRange;
	private List<Location> locationList;
	private List<EventType> eventTypeList;
	private List<EventStatusEnum> eventStatusList;
	private LocalDate startDate;
	private LocalDate endDate;

	public EventListFilter() {
	}

	public EventListFilter(CostRange costRange, List<Location> locationList, List<EventType> eventTypeList,
		List<EventStatusEnum> eventStatusList, LocalDate startDate, LocalDate endDate) {
		this.costRange = costRange;
		this.locationList = locationList;
		this.eventTypeList = eventTypeList;
		this.eventStatusList = eventStatusList;
		this.startDate = startDate;
		this.endDate = endDate;
	}
}

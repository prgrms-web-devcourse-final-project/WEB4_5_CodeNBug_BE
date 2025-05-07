package org.codeNbug.mainserver.domain.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Location {

	@Column(nullable = false, name = "location")
	private String location;

	public Location(String location) {
		this.location = location;
	}

	public Location() {
	}

	public String getLocation() {
		return location;
	}
}

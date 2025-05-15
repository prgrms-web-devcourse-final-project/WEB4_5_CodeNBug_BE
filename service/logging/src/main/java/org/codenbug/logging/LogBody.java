package org.codenbug.logging;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class LogBody {
	private String userId;
	private String role;
	private String ipAddr;
	private String description;
	private ResultStatus status;

	public LogBody(String userId, String role, String ipAddr, String description, ResultStatus status) {
		this.userId = userId;
		this.role = role;
		this.ipAddr = ipAddr;
		this.description = description;
		this.status = status;
	}

	@Override
	public String toString() {
		return """
			%s
			userId = %s
			role = %s
			ipAddr = %s
			description = %s
			""".formatted(status, userId, role, ipAddr, description);
	}
}

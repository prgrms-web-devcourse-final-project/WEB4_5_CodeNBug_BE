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
	private String method;
	private String className;

	public LogBody(String userId, String role, String ipAddr, String description, ResultStatus status, String method,
		String className) {
		this.userId = userId;
		this.role = role;
		this.ipAddr = ipAddr;
		this.description = description;
		this.status = status;
		this.method = method;
		this.className = className;
	}

	@Override
	public String toString() {
		return """
			
			%s %s : %s
			userId = %s
			role = %s
			ipAddr = %s
			description = %s
			""".formatted(className, method, status, userId, role, ipAddr, description);
	}
}

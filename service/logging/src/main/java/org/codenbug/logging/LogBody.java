package org.codenbug.logging;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class LogBody {
	private Long userId;
	private String role;
	private String ipAddr;
	private String description;
	private ResultStatus status;
}

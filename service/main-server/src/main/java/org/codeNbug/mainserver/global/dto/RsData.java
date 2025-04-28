package org.codeNbug.mainserver.global.dto;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RsData<T> {
	@NonNull
	private String code;
	@NonNull
	private String msg;
	@NonNull
	private T data;

	public RsData(String code, String msg) {
		this(code, msg, (T)new Empty());
	}

	@JsonIgnore
	public int getStatusCode() {
		String statusCodeStr = code.split("-")[0];
		return Integer.parseInt(statusCodeStr);
	}
}

package org.codenbug.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Util {
	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static <T> T fromJson(String json, Class<T> type) {
		try {
			return objectMapper.readValue(json, type);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse JSON to " + type.getSimpleName(), e);
		}
	}

	public static <T> T fromJson(String json, TypeReference<T> typeReference) {
		try {
			return objectMapper.readValue(json, typeReference);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse JSON", e);
		}
	}

	public static String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert object to JSON", e);
		}
	}

	public static <T> T convertValue(Object fromValue, Class<T> type) {
		try {
			return objectMapper.convertValue(fromValue, type);
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert value to " + type.getSimpleName(), e);
		}
	}

	public static <T> T convertValue(Object fromValue, TypeReference<T> typeReference) {
		try {
			return objectMapper.convertValue(fromValue, typeReference);
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert value", e);
		}
	}

}

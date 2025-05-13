package org.codeNbug.mainserver.domain.event.entity;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

	private String startDateFieldName;
	private String endDateFieldName;

	@Override
	public void initialize(ValidDateRange constraintAnnotation) {
		this.startDateFieldName = constraintAnnotation.startDateFieldName();
		this.endDateFieldName = constraintAnnotation.endDateFieldName();
	}

	@Override
	public boolean isValid(Object object, ConstraintValidatorContext context) {
		try {
			Field startDateField = object.getClass().getDeclaredField(startDateFieldName);
			Field endDateField = object.getClass().getDeclaredField(endDateFieldName);

			startDateField.setAccessible(true);
			endDateField.setAccessible(true);

			LocalDateTime startDate = (LocalDateTime)startDateField.get(object);
			LocalDateTime endDate = (LocalDateTime)endDateField.get(object);

			// 둘 다 null이면 유효하다고 간주
			if (startDate == null || endDate == null) {
				return true;
			}

			// startDate가 endDate보다 이전이거나 같은지 확인
			return !startDate.isAfter(endDate);

		} catch (Exception e) {
			return false;
		}
	}
}

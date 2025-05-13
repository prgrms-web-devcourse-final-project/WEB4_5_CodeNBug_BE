package org.codeNbug.mainserver.domain.event.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
	String message() default "시작일은 종료일보다 이전이어야 합니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	String startDateFieldName() default "startDate";

	String endDateFieldName() default "endDate";
}

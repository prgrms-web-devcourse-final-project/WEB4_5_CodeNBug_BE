package org.codenbug.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 주요 비즈니스 호출 로직을 출력하는 어노테이션 <br/>
 * {@code info} 레벨로 출력한다.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessLogging {
	String description() default "";
}

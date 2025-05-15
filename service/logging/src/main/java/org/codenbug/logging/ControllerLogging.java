package org.codenbug.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 호출의 로깅을 담당하는 어노테이션 <br/>
 * 이 어노테이션이 붙어 있으면 컨트롤러 호출의 결과를 {@code info} 레벨로 출력한다.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ControllerLogging {
	String description() default "";
}

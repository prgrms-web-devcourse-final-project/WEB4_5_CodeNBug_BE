package org.codenbug.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class LoggingAop {
	public LoggingAop() {
		log.info("LoggingAop created");
	}

	@After("@within(ControllerLogging) ** execution(public * *(..))")
	public void controllerLogging(JoinPoint joinPoint) {
		log.info("Controller executed: {}.{}",
			joinPoint.getSignature().getDeclaringTypeName(),
			joinPoint.getSignature().getName());
	}
}

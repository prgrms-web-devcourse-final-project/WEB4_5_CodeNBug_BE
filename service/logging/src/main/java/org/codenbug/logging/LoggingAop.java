package org.codenbug.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class LoggingAop {
	private final HttpServletRequest request;

	public LoggingAop(HttpServletRequest request) {
		this.request = request;
		log.info("LoggingAop created");
	}

	@Around("@within(ControllerLogging) && execution(public * *(..))")
	public Object controllerLogging(ProceedingJoinPoint joinPoint) throws Throwable {
		String userId = "anonymous";
		String role = "anonymous";
		String ipAddr = request.getRemoteAddr();
		String methodName = joinPoint.getTarget().getClass().getDeclaredMethods()[0].getName();
		String className = joinPoint.getTarget().getClass().getSimpleName();
		String uri = request.getRequestURI();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null) {
			Object principal = authentication.getPrincipal();
			if (principal instanceof UserDetails userdetail) {
				userId = userdetail.getUsername();
				role = userdetail.getAuthorities().stream()
					.map(Object::toString)
					.reduce((a, b) -> a + "," + b)
					.orElse("anonymous");
			}
		}

		try {
			Object proceed = joinPoint.proceed();
			log.info("{}", new LogBody(
				userId,
				role,
				ipAddr,
				String.format("%s.%s 호출 성공", className, methodName),
				ResultStatus.SUCCESS
			));
			return proceed;
		} catch (Throwable e) {
			log.error("{}", new LogBody(
				userId,
				role,
				ipAddr,
				String.format("%s.%s 호출 실패", className, methodName),
				ResultStatus.FAIL
			));
			throw e;
		}
	}
}

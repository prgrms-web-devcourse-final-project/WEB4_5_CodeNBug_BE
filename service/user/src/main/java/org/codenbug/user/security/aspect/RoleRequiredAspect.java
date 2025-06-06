package org.codenbug.user.security.aspect;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.security.annotation.RoleRequired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class RoleRequiredAspect {

	@Around("@annotation(roleRequired) || @within(roleRequired)")
	public Object checkRole(ProceedingJoinPoint joinPoint, RoleRequired roleRequired) throws Throwable {
		// 메서드 레벨 어노테이션이 없으면 클래스 레벨 어노테이션 확인
		if (roleRequired == null) {
			MethodSignature signature = (MethodSignature)joinPoint.getSignature();
			Method method = signature.getMethod();

			roleRequired = method.getAnnotation(RoleRequired.class);

			if (roleRequired == null) {
				roleRequired = method.getDeclaringClass().getAnnotation(RoleRequired.class);
			}
		}

		if (roleRequired == null) {
			return joinPoint.proceed();
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			log.warn("Unauthorized access attempt to protected method: {}", joinPoint.getSignature());
			throw new AccessDeniedException("Authentication is required to access this resource");
		}

		UserRole[] requiredRoles = roleRequired.value();
		log.debug("Checking roles: required={}, method={}",
			Arrays.toString(requiredRoles), joinPoint.getSignature());

		boolean hasRequiredRole = Arrays.stream(requiredRoles)
			.anyMatch(role -> authentication.getAuthorities().contains(
				new SimpleGrantedAuthority(role.getRole())));

		if (!hasRequiredRole) {
			log.warn("Access denied: user={}, required roles={}, method={}",
				authentication.getName(), Arrays.toString(requiredRoles), joinPoint.getSignature());
			throw new AccessDeniedException("You don't have the required role(s): " + Arrays.toString(requiredRoles));
		}

		log.debug("Access granted: user={}, method={}",
			authentication.getName(), joinPoint.getSignature());

		return joinPoint.proceed();
	}
}
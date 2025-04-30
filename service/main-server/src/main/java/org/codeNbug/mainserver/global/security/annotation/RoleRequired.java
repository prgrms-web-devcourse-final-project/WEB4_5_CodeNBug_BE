package org.codeNbug.mainserver.global.security.annotation;

import org.codeNbug.mainserver.domain.user.constant.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleRequired {
    UserRole[] value() default {UserRole.USER};
} 
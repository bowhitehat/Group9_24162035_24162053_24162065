package com.group9.topicmanagement.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect @Component
public class SecurityAspect {
    private static final Logger log = LoggerFactory.getLogger(SecurityAspect.class);
    @Before("execution(public * com.group9.topicmanagement.web..*(..))")
    public void audit(JoinPoint point) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("SECURITY actor={} action={}", authentication == null ? "anonymous" : authentication.getName(), point.getSignature().toShortString());
    }
}

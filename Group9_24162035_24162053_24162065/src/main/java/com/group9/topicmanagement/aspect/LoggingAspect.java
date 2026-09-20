package com.group9.topicmanagement.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect @Component @Order(Ordered.LOWEST_PRECEDENCE)
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    @Around("execution(public * com.group9.topicmanagement.service..*(..)) || execution(public * com.group9.topicmanagement.web..*(..))")
    public Object logCall(ProceedingJoinPoint point) throws Throwable {
        long start = System.nanoTime(); String name = point.getSignature().toShortString();
        try { Object result = point.proceed(); log.debug("Hoàn tất {} trong {} ms", name, elapsed(start)); return result; }
        catch (Throwable error) { log.warn("Thất bại {} trong {} ms: {}", name, elapsed(start), error.getMessage()); throw error; }
    }
    private long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }
}

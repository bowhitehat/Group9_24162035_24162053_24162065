package com.group9.topicmanagement.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect @Component
public class TransactionAspect {
    private static final Logger log = LoggerFactory.getLogger(TransactionAspect.class);
    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || @within(org.springframework.transaction.annotation.Transactional)")
    public Object monitor(ProceedingJoinPoint point) throws Throwable {
        long start = System.nanoTime();
        try { return point.proceed(); }
        finally { log.debug("TRANSACTION {} ms: {}", (System.nanoTime() - start) / 1_000_000, point.getSignature().toShortString()); }
    }
}

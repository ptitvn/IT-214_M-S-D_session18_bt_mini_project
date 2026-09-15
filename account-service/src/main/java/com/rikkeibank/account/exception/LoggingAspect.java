package com.rikkeibank.account.exception;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.rikkeibank.account.controller..*(..)) || execution(* com.rikkeibank.account.service..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("{} executed in {}ms", joinPoint.getSignature().toShortString(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.error("{} threw {}: {}", joinPoint.getSignature().toShortString(), ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}

package com.duyvu.distributed.lock.aop;

import com.duyvu.distributed.lock.annotation.DistributedLock;
import com.duyvu.distributed.lock.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Aspect
@Component
@RequiredArgsConstructor
public class LockingAspect {
    private final DistributedLockService lockService;

    @Around("@annotation(distributedLock)")
    public Object handleDistributedLock(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
        boolean isAcquired = lockService.acquire(Duration.ofSeconds(distributedLock.timeout()));

        if (!isAcquired) {
            throw new TimeoutException("Cannot acquire lock in time " + Duration.ofSeconds(distributedLock.timeout()));
        }

        try {
            return pjp.proceed();
        } finally {
            lockService.release();
        }
    }
}

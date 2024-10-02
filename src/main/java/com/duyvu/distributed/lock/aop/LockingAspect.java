package com.duyvu.distributed.lock.aop;

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

    @Around("@annotation(com.duyvu.distributed.lock.annotation.DistributedLock)")
    public Object handleDistributedLock(ProceedingJoinPoint pjp) throws Throwable {
        // TODO: fix busy-waiting by implement pub/sub for await
        Duration timeout = Duration.ofSeconds(2);
        boolean isAcquired = lockService.acquire(timeout);

        if (!isAcquired) {
            throw new TimeoutException("Cannot acquire lock in time " + timeout);
        }

        try {
            return pjp.proceed();
        } finally {
            lockService.release();
        }
    }
}

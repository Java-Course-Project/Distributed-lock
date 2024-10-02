package com.duyvu.distributed.lock.aop;

import com.duyvu.distributed.lock.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class LockingAspect {
    private final DistributedLockService lockService;

    @SuppressWarnings("BusyWait")
    @Around("@annotation(com.duyvu.distributed.lock.annotation.DistributedLock)")
    public Object handleDistributedLock(ProceedingJoinPoint pjp) throws Throwable {
        // TODO: fix busy-waiting by implement pub/sub for await
        while (!lockService.tryAcquire()) {
            Thread.sleep(500);
        }

        try {
            return pjp.proceed();
        } finally {
            lockService.release();
        }
    }
}

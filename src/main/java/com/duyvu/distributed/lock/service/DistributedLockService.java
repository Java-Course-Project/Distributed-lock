package com.duyvu.distributed.lock.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class DistributedLockService {
    private static final String LOCK_KEY = "distributed-lock";
    private static final String LOCK_VALUE = UUID.randomUUID().toString();
    private static final Duration EXPIRED_TIME = Duration.ofSeconds(10);
    private static final Duration RENEW_TIME = EXPIRED_TIME.dividedBy(5);
    private ScheduledFuture<?> futureTask = null;

    private final RedisTemplate<String, String> redisTemplate;

    public DistributedLockService(@Qualifier("redisLockTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public synchronized boolean tryAcquire() {
        boolean acquired = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, LOCK_VALUE, EXPIRED_TIME));
        if (acquired && futureTask == null) {
            futureTask = Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(
                    () -> redisTemplate.opsForValue().getAndExpire(LOCK_KEY, EXPIRED_TIME), RENEW_TIME.toSeconds(), RENEW_TIME.toSeconds(),
                    TimeUnit.SECONDS);
        }
        return acquired;
    }

    // TODO: fix busy-waiting by implement pub/sub for await
    @SuppressWarnings("BusyWait")
    public boolean acquire(Duration timeout) {
        Instant startTime = Instant.now();
        while (!tryAcquire()) {
            if (!(timeout.isNegative() || timeout.isZero())) {
                if (startTime.plus(timeout).isAfter(Instant.now())) {
                    return false;
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    public synchronized void release() {
        if (Objects.equals(redisTemplate.opsForValue().get(LOCK_KEY), LOCK_VALUE)) {
            redisTemplate.delete(LOCK_KEY);
            if (futureTask != null) {
                futureTask.cancel(true);
                futureTask = null;
            }
        }
    }
}

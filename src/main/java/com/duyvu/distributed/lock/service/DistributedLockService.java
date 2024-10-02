package com.duyvu.distributed.lock.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class DistributedLockService {
    private static final String LOCK_KEY = "distributed-lock";
    private static final String LOCK_VALUE = UUID.randomUUID().toString();
    private static final Duration EXPIRED_TIME = Duration.ofSeconds(30);

    private final RedisTemplate<String, String> redisTemplate;

    public DistributedLockService(@Qualifier("redisLockTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // TODO: implement a watch service to auto expire lock
    public boolean tryAcquire() {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, LOCK_VALUE, EXPIRED_TIME));
    }

    @SuppressWarnings("BusyWait")
    public boolean acquire(Duration timeout) {
        Instant startTime = Instant.now();
        while (!tryAcquire()) {
            if (!(timeout.isNegative() || timeout.isZero())) {
                if (startTime.plus(timeout).isAfter(Instant.now()) ) {
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

    public void release() {
        if (Objects.equals(redisTemplate.opsForValue().get(LOCK_KEY), LOCK_VALUE)) {
            redisTemplate.delete(LOCK_KEY);
        }
    }
}

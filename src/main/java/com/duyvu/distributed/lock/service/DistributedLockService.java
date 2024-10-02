package com.duyvu.distributed.lock.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    public void release() {
        redisTemplate.delete(LOCK_KEY);
    }
}

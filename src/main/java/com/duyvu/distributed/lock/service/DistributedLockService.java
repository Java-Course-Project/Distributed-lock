package com.duyvu.distributed.lock.service;

import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DistributedLockService implements MessageListener {
    private static final String LOCK_KEY = "distributed-lock";
    private static final String LOCK_VALUE = UUID.randomUUID().toString();
    private static final Duration EXPIRED_TIME = Duration.ofSeconds(10);
    private static final Duration RENEW_TIME = EXPIRED_TIME.dividedBy(5);
    private ScheduledFuture<?> renewLockExpireTimeTask = null;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final String lockChannel;

    private final RedisTemplate<String, String> redisTemplate;

    public DistributedLockService(@Value("${application.lock.channel}") String lockChannel,
                                  @Qualifier("redisLockTemplate") RedisTemplate<String, String> redisTemplate) {
        this.lockChannel = lockChannel;
        this.redisTemplate = redisTemplate;
    }

    public synchronized boolean tryAcquire() {
        boolean acquired = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, LOCK_VALUE, EXPIRED_TIME));
        if (acquired && renewLockExpireTimeTask == null) {
            renewLockExpireTimeTask = Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(
                    () -> redisTemplate.opsForValue().getAndExpire(LOCK_KEY, EXPIRED_TIME), RENEW_TIME.toSeconds(), RENEW_TIME.toSeconds(),
                    TimeUnit.SECONDS);
        }
        return acquired;
    }

    public boolean acquire(Duration timeout) {
        lock.lock();
        try {
            while (!tryAcquire()) {
                boolean timeoutExpired = condition.await(timeout.toSeconds(), TimeUnit.SECONDS);
                if (!timeoutExpired) {
                    return false;
                }
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private void await() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }

    }

    public synchronized void release() {
        if (Objects.equals(redisTemplate.opsForValue().get(LOCK_KEY), LOCK_VALUE)) {
            redisTemplate.delete(LOCK_KEY);
            // publish for all subscribers that key was release for other instances to get key
            redisTemplate.convertAndSend(lockChannel, LOCK_VALUE);
            if (renewLockExpireTimeTask != null) {
                renewLockExpireTimeTask.cancel(true);
                renewLockExpireTimeTask = null;
            }
        }
    }

    @Override
    public void onMessage(@Nonnull Message message, byte[] pattern) {
        await();
    }
}

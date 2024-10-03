package com.duyvu.distributed.lock.service;

import com.duyvu.distributed.lock.annotation.DistributedLock;
import com.duyvu.distributed.lock.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class ScheduledService {
    private final String applicationName;
    private final RedisTemplate<Long, Boolean> redisTemplate;

    public ScheduledService(@Value("${spring.application.name}") String applicationName, RedisTemplate<Long, Boolean> redisTemplate) {
        this.applicationName = applicationName;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(cron = "0/5 * * * * ?")
    @DistributedLock(timeout = 2)
    public void run() {
        Instant roundedTime = TimeUtil.getCurrentTimeRoundToNearest5Second();
        boolean wasTaskRun = Boolean.TRUE.equals(redisTemplate.opsForValue().get(roundedTime.toEpochMilli()));
        if (!wasTaskRun) {
            log.debug("Running task at instance {}", applicationName);
            redisTemplate.opsForValue().set(roundedTime.toEpochMilli(), Boolean.TRUE);
        }
    }
}

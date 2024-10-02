package com.duyvu.distributed.lock.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeUtil {

    public static Instant getCurrentTimeRoundToNearest5Second() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());

        int seconds = dateTime.getSecond();
        int roundedSeconds = (seconds / 5) * 5;

        dateTime = dateTime.withSecond(roundedSeconds).withNano(0);

        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}

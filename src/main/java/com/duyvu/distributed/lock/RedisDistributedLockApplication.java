package com.duyvu.distributed.lock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RedisDistributedLockApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisDistributedLockApplication.class, args);
	}

}

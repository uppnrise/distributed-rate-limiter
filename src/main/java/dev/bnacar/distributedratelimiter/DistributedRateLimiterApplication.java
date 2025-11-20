package dev.bnacar.distributedratelimiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DistributedRateLimiterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedRateLimiterApplication.class, args);
    }

}

package dev.bnacar.distributedratelimiter;

import org.springframework.boot.SpringApplication;

public class TestDistributedRateLimiterApplication {

    public static void main(String[] args) {
        SpringApplication.from(DistributedRateLimiterApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

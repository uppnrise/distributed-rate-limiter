package dev.bnacar.distributedratelimiter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Distributed Rate Limiter API")
                        .description("A comprehensive distributed token bucket rate limiter implementation with Redis support, " +
                                   "featuring configurable capacity and refill rates, thread-safe operations, " +
                                   "performance monitoring, and administrative controls.")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("Distributed Rate Limiter")
                                .url("https://github.com/uppnrise/distributed-rate-limiter"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development server"),
                        new Server().url("/").description("Current server")
                ));
    }
}
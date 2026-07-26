package dev.bnacar.distributedratelimiter.ratelimit;

import java.util.List;

/**
 * Bridges Spring Data Redis's runtime {@code List.class} result token to a
 * parameterized result type without leaking raw types into rate limiter code.
 */
final class RedisScriptResultTypes {

    private RedisScriptResultTypes() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static Class<List<Object>> objectList() {
        return (Class) List.class;
    }
}

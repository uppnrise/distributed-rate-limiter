package dev.bnacar.distributedratelimiter.loadtest

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Stress test for the Distributed Rate Limiter service.
 * Tests system behavior under extreme load conditions.
 */
class StressTest extends Simulation {

  val httpProtocol = http
    .baseUrl(System.getProperty("stress.test.baseUrl", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("GatlingStressTest/1.0")
    .maxConnectionsPerHost(100)

  // Stress test configuration
  val stressTestDuration = Integer.getInteger("stress.test.duration", 60).intValue.seconds
  val maxUsers = Integer.getInteger("stress.test.maxUsers", 200).intValue
  val rampUpDuration = Integer.getInteger("stress.test.rampUp", 20).intValue.seconds
  val plateauDuration = Integer.getInteger("stress.test.plateau", 30).intValue.seconds

  // High-frequency rate limit scenario
  val highFrequencyScenario = scenario("High Frequency Rate Limiting")
    .during(stressTestDuration) {
      exec(http("high-freq-rate-limit")
        .post("/api/ratelimit/check")
        .body(StringBody("""{"key": "stress-test-${__threadNum()}", "tokensRequested": 1}""")).asJson
        .check(status.in(200, 429)))
        .pause(10.milliseconds, 50.milliseconds)
    }

  // Burst traffic scenario
  val burstTrafficScenario = scenario("Burst Traffic")
    .during(stressTestDuration) {
      repeat(5) {
        exec(http("burst-request")
          .post("/api/ratelimit/check")
          .body(StringBody("""{"key": "burst-${__UUID()}", "tokensRequested": 5}""")).asJson
          .check(status.in(200, 429)))
      }
      .pause(1.second, 3.seconds)
    }

  // Concurrent benchmark scenario
  val concurrentBenchmarkScenario = scenario("Concurrent Benchmarks")
    .during(stressTestDuration) {
      exec(http("concurrent-benchmark")
        .post("/api/benchmark/run")
        .body(StringBody("""{
          "concurrentThreads": 10,
          "requestsPerThread": 100,
          "durationSeconds": 10,
          "tokensPerRequest": 1,
          "keyPrefix": "stress-bench-${__threadNum()}"
        }""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.success").is("true")))
        .pause(5.seconds, 15.seconds)
    }

  // Mixed workload scenario
  val mixedWorkloadScenario = scenario("Mixed Workload")
    .during(stressTestDuration) {
      randomSwitch(
        40.0 -> exec(http("mixed-rate-limit")
          .post("/api/ratelimit/check")
          .body(StringBody("""{"key": "mixed-${__UUID()}", "tokensRequested": 1}""")).asJson
          .check(status.in(200, 429))),
        
        30.0 -> exec(http("mixed-health")
          .get("/actuator/health")
          .check(status.is(200))),
        
        20.0 -> exec(http("mixed-metrics")
          .get("/actuator/metrics")
          .check(status.is(200))),
        
        10.0 -> exec(http("mixed-config")
          .get("/api/ratelimit/config")
          .check(status.is(200)))
      ).pause(50.milliseconds, 200.milliseconds)
    }

  // Resource exhaustion test
  val resourceExhaustionScenario = scenario("Resource Exhaustion")
    .during(stressTestDuration) {
      exec(http("resource-test")
        .post("/api/ratelimit/check")
        .body(StringBody("""{"key": "resource-${__UUID()}", "tokensRequested": 10}""")).asJson
        .check(status.in(200, 429, 503)))
        .pause(1.millisecond, 10.milliseconds)
    }

  // Stress test setup with multiple phases
  setUp(
    // Phase 1: Normal load
    highFrequencyScenario.inject(
      rampUsers(maxUsers / 8).during(rampUpDuration / 4)
    ).protocols(httpProtocol),
    
    burstTrafficScenario.inject(
      rampUsers(maxUsers / 8).during(rampUpDuration / 4)
    ).protocols(httpProtocol),
    
    // Phase 2: Increased load
    mixedWorkloadScenario.inject(
      nothingFor(rampUpDuration / 4),
      rampUsers(maxUsers / 4).during(rampUpDuration / 2)
    ).protocols(httpProtocol),
    
    // Phase 3: Peak load
    resourceExhaustionScenario.inject(
      nothingFor(rampUpDuration / 2),
      rampUsers(maxUsers / 2).during(rampUpDuration / 2),
      constantUsersPerSec(maxUsers / 2).during(plateauDuration)
    ).protocols(httpProtocol),
    
    // Phase 4: Sustained benchmark load
    concurrentBenchmarkScenario.inject(
      nothingFor(rampUpDuration),
      rampUsers(maxUsers / 10).during(rampUpDuration / 4),
      constantUsersPerSec(maxUsers / 10).during(plateauDuration)
    ).protocols(httpProtocol)
  ).assertions(
    // Relaxed assertions for stress testing
    global.responseTime.max.lt(2000),
    global.responseTime.mean.lt(1000),
    global.responseTime.percentile3.lt(1500),
    global.successfulRequests.percent.gt(85.0), // Lower threshold for stress conditions
    forAll.failedRequests.count.lt(100)
  ).maxDuration(stressTestDuration + rampUpDuration + 30.seconds)
}
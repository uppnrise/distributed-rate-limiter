package dev.bnacar.distributedratelimiter.loadtest

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Basic load test for the Distributed Rate Limiter service.
 * Tests the rate limiting endpoints under various load conditions.
 */
class BasicLoadTest extends Simulation {

  // HTTP configuration
  val httpProtocol = http
    .baseUrl(System.getProperty("load.test.baseUrl", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("GatlingLoadTest/1.0")

  // Test scenarios configuration
  val loadTestDuration = Integer.getInteger("load.test.duration", 30).intValue.seconds
  val maxUsers = Integer.getInteger("load.test.maxUsers", 50).intValue
  val rampUpDuration = Integer.getInteger("load.test.rampUp", 10).intValue.seconds

  // Health check scenario
  val healthCheckScenario = scenario("Health Check")
    .exec(http("health-check")
      .get("/actuator/health")
      .check(status.is(200)))

  // Rate limit check scenario
  val rateLimitScenario = scenario("Rate Limit Test")
    .exec(http("rate-limit-check")
      .post("/api/ratelimit/check")
      .body(StringBody("""{"key": "load-test-${__gatlingUserId()}-${__counter()}", "tokens": 1}""")).asJson
      .check(status.in(200, 429))
      .check(jsonPath("$.allowed").exists))

  // Benchmark scenario
  val benchmarkScenario = scenario("Benchmark Test")
    .exec(http("benchmark-run")
      .post("/api/benchmark/run")
      .body(StringBody("""{
        "concurrentThreads": 5,
        "requestsPerThread": 50,
        "durationSeconds": 5,
        "tokensPerRequest": 1,
        "keyPrefix": "gatling-test"
      }""")).asJson
      .check(status.is(200))
      .check(jsonPath("$.success").is("true"))
      .check(jsonPath("$.throughputPerSecond").exists))

  // Metrics collection scenario
  val metricsScenario = scenario("Metrics Collection")
    .exec(http("metrics-check")
      .get("/actuator/metrics")
      .check(status.is(200))
      .check(jsonPath("$.names").exists))

  // Performance thresholds for assertions
  val responseTimeThreshold = Integer.getInteger("load.test.responseTime.threshold", 500).intValue
  val successRateThreshold = java.lang.Double.parseDouble(System.getProperty("load.test.successRate.threshold", "95.0"))

  // Load test setup
  setUp(
    healthCheckScenario.inject(
      rampUsers(maxUsers / 4).during(rampUpDuration),
      constantUsersPerSec(maxUsers / 4).during(loadTestDuration)
    ).protocols(httpProtocol),
    
    rateLimitScenario.inject(
      rampUsers(maxUsers / 2).during(rampUpDuration),
      constantUsersPerSec(maxUsers / 2).during(loadTestDuration)
    ).protocols(httpProtocol),
    
    benchmarkScenario.inject(
      rampUsers(maxUsers / 8).during(rampUpDuration),
      constantUsersPerSec(maxUsers / 8).during(loadTestDuration)
    ).protocols(httpProtocol),
    
    metricsScenario.inject(
      rampUsers(maxUsers / 8).during(rampUpDuration),
      constantUsersPerSec(maxUsers / 8).during(loadTestDuration)
    ).protocols(httpProtocol)
  ).assertions(
    global.responseTime.max.lt(responseTimeThreshold),
    global.responseTime.mean.lt(responseTimeThreshold / 2),
    global.successfulRequests.percent.gt(successRateThreshold),
    forAll.failedRequests.count.lt(10)
  )
}
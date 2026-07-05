package io.github.dmitriyiliyov.ipratelimiter.loadtest;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.incrementUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class LoadSimulation extends Simulation {

  static Integer vu = 50;
  static Duration duration = Duration.ofSeconds(10);

  static HttpProtocolBuilder httpProtocol = http
          .baseUrl("http://localhost:8080")
          .acceptHeader("application/json")
          .contentTypeHeader("application/json");

  static final ScenarioBuilder scn =
          scenario("Load scenario")
          .exec(http("load_scenario")
                  .get("/api/test-ip-rate-limiter/guarded")
                  .check(status().is(200))
          );

  {
    setUp(
            scn.injectOpen(
                    incrementUsersPerSec(vu)
                            .times(4)
                            .eachLevelLasting(duration)
                            .separatedByRampsLasting(5)
                            .startingFrom(10)
            )
    ).protocols(httpProtocol);
  }
}

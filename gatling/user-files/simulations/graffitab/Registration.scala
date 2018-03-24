package graffitab

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
  * Created by david on 11/10/2016.
  */
class Registration extends Simulation {

  // Queue: default behaviour, pop first from the queue -- needs enough records
  val feeder = csv("users.csv").queue

  val headers = Map("Content-Type" -> "application/json")

  val register = feed(feeder)
    .exec(http("Sign up")
    .post("/v1/users")
    .headers(headers)
    .body(ElFileBody("user.json")).asJSON
    .check(status is 201)
    .check(jsonPath("$.result").ofType[String] is "OK"))

  val scn = scenario("Sign up into GraffiTab").exec(register)

  setUp(scn.inject(rampUsers(150) over (30 seconds)).protocols(TestConfig.httpConf))

}

/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package graffitab

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

object Login extends Simulation {

  //object Login {

    val headers = Map("Content-Type" -> "application/json")

    val login = exec(http("Login")
        .post("/v1/login")
        .headers(headers)
        .body(ElFileBody("login.json")).asJSON
        .check(status is 200)
        .check(jsonPath("$.user.id").ofType[Int].exists)
        .check(header(HttpHeaderNames.SetCookie).exists.saveAs("sessionCookie")))
        .exec(session => {
            println(session("sessionCookie").as[String])
            session
          })

    val me =  exec(http("Me")
      .get("/v1/users/me")
      .check(status is 200)
      .check(jsonPath("$.user.id").ofType[Int].exists))
  //}

  // Now, we can write the scenario as a composition
  val scn = scenario("Login into GraffiTab").exec(Login.login, Login.me)

  // atOnceUsers(1)
  // 1000 users over 90 seconds
  setUp(scn.inject(rampUsers(10) over (10 seconds)).protocols(TestConfig.httpConf))
}

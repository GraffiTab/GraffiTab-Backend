package graffitab

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
  * Created by david on 13/10/2016.
  */
object Streamable {
  val newest = exec(http("Me")
    .get("/api/users/me")
    .check(status is 200)
    .check(jsonPath("$.user.id").ofType[Int].exists))
}

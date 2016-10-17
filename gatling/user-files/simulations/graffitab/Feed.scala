package graffitab

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
  * Created by david on 13/10/2016.
  */
object Feed {

  val getFeed = exec(http("Get My Feed")
    .get("/api/users/me/feed")
    .check(status is 200)
    .check(jsonPath("$.items").exists))

  val trending = exec(http("Trending")
    .get("/api/streamables/popular")
    .check(status is 200)
    .check(jsonPath("$.items").exists))
}

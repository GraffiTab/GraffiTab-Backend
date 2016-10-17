package graffitab

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
  * Created by david on 11/10/2016.
  */
class Upload extends Simulation {

  // Queue: default behaviour, pop first from the queue -- needs enough records
  val feeder = csv("users.csv").queue

  val uploadAvatar = feed(feeder)
    .exec(Login.login)
    .pause(2)
    .exec(http("Upload Avatar")
    .post("/api/users/me/avatar")
      .bodyPart(RawFileBodyPart("file","testAvatar.png").contentType("image/png")).asMultipartForm
    .check(status is 200)
    .check(jsonPath("$.asset.guid").ofType[String].exists))

  val uploadCover = pause(2)
    .exec(http("Upload Cover")
      .post("/api/users/me/cover")
      .bodyPart(RawFileBodyPart("file","testCover.jpg").contentType("image/jpeg")).asMultipartForm
      .check(status is 200)
      .check(jsonPath("$.asset.guid").ofType[String].exists))

  val scn = scenario("Upload Avatar into GraffiTab").exec(uploadAvatar, uploadCover)

  setUp(scn.inject(rampUsers(150) over (30 seconds)).protocols(TestConfig.httpConf))
}

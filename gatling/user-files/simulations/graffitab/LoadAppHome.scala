package graffitab

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
  * Created by david on 11/10/2016.
  */
class LoadAppHome extends Simulation {

  // Queue: default behaviour, pop first from the queue -- needs enough records
  val feeder = csv("users.csv").queue

  val headers = Map("Content-Type" -> "application/json")

  val loadAppHome = feed(feeder)
            .exec(Login.login)
            .pause(300 milliseconds)
            .exec(Login.me)
            .pause(300 milliseconds)
            .exec(Notification.unseenNotifications)
            .pause(300 milliseconds)
            .exec(Feed.getFeed)
            .pause(300 milliseconds)
            .exec(Notification.getNotifications)
            .pause(300 milliseconds)
            .exec(Feed.trending)
            .pause(300 milliseconds)
            .exec(Streamable.newest)

  val scn = scenario("Load GraffiTab App").exec(loadAppHome)

  setUp(scn.inject(rampUsers(150) over (30 seconds)).protocols(TestConfig.httpConf))

}

package graffitab

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
  * Created by david on 13/10/2016.
  */
object Notification {

  val unseenNotifications = exec(http("Unread notifications")
    .get("/api/users/me/notifications/unreadcount")
    .check(status is 200)
    .check(jsonPath("$.count").ofType[Int].exists))

  val getNotifications = exec(http("Notifications")
    .get("/api/users/me/notifications")
    .check(status is 200)
    .check(jsonPath("$.items").exists))

}

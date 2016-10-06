package graffitab

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
  * Created by david on 11/10/2016.
  */
object TestConfig {

  val url = "https://dev.graffitab.com"
  //val url = "http://localhost:8080"

  val httpConf = http
    .baseURL(url)
    .acceptHeader("application/json")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
}

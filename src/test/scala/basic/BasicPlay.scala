package basic

import io.gatling.core.Predef._
import io.gatling.core.session.SessionAttribute
import io.gatling.http.Predef._

import scala.concurrent.duration._

class BasicPlay extends Simulation {

  val auth = "authenticate"
  val org = "Demo"
  val gameid = 7316
  val play = "play"
  val currency = "EUR"
  val amount = 25
  val coin = 1

  val httpConf = http
    .baseURL("https://dev.yggdrasil.lan/game.web") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val scn = scenario("Basic test, play till win") // A scenario is a chain of requests and pauses
    .exec(session => session.set("command", "N"))
    .exec(http("Authentication")
      .post("/service")
      .queryParam("fn", auth)
      .queryParam("org", org)
      .queryParam("gameid", gameid)
      .check(jsonPath("$.data.sessid").saveAs("sessionId"))
    )
    .asLongAs(session => session("command").as[String] != "C"){
      exec(http("Playing")
        .get("/service")
        .queryParam("fn", play)
        .queryParam("currency", currency)
        .queryParam("gameid", gameid)
        .queryParam("sessid", "${sessionId}")
        .queryParam("amount", amount)
        .queryParam("coin", coin)
        .check(jsonPath("$.data.wager.wagerid").saveAs("wagerId"))
        .check(jsonPath("$.data.wager.bets[0].eventdata.nextCmds").optional.saveAs("command"))
      )
    }
    .exec(http("Collecting")
      .get("/service")
      .queryParam("fn", play)
      .queryParam("currency", currency)
      .queryParam("gameid", gameid)
      .queryParam("sessid", "${sessionId}")
      .queryParam("wagerid", "${wagerId}")
      .queryParam("cmd", "${command}")
      .queryParam("betid", 1)
      .queryParam("step", 2)
      .check(jsonPath("$.data.wager.bets[0].wonamount").saveAs("wonAmount"))
    )
    .exec{
      session =>
        println(session("wagerId").as[Int])
        session
    }
    .exec{
      session =>
        println(session("command").as[String])
        session
    }
    .exec{
      session =>
        println(session("wonAmount").as[Double])
        session
    }


  setUp(
    scn.inject(atOnceUsers(1)
    ).protocols(httpConf))

}

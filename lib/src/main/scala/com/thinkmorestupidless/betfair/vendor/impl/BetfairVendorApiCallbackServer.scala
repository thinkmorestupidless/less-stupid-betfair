package com.thinkmorestupidless.betfair.vendor.impl

import com.thinkmorestupidless.betfair.vendor.domain.Code
import com.thinkmorestupidless.betfair.vendor.impl.JsonCodecs._
import com.thinkmorestupidless.utils.HttpServer
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

object BetfairVendorApiCallbackServer {

  def start(port: Int)(implicit system: ActorSystem): Unit =
    HttpServer.start(
      classOf[BetfairVendorApiCallbackServer.type].getSimpleName,
      BetfairVendorApiCallbackServerRoutes.create(),
      port
    )
}

object BetfairVendorApiCallbackServerRoutes {
  import com.thinkmorestupidless.utils.CirceSupport._

  def create(): Route =
    path("code")(post {
      entity(as[Code]) { code =>
        complete {}
      }
    })
}

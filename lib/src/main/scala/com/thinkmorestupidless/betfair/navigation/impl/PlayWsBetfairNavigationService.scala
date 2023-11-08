package com.thinkmorestupidless.betfair.navigation.impl

import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.betfair.navigation.domain.{BetfairNavigationService, Menu}
import org.apache.pekko.actor.ActorSystem
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import spray.json._

import scala.concurrent.Future

final class PlayWsBetfairNavigationService(config: BetfairConfig)(implicit system: ActorSystem)
    extends BetfairNavigationService
    with JsonCodecs {

  private implicit val ec = system.dispatcher
  private val wsClient = StandaloneAhcWSClient()

  override def menu()(implicit session: BetfairSession): Future[Menu] =
    wsClient
      .url(config.navigation.uri.value)
      .addHttpHeaders(
        (config.headerKeys.applicationKey.value, session.applicationKey.value),
        (config.headerKeys.sessionToken.value, session.sessionToken.value)
      )
      .get()
      .map { response =>
        response.body.parseJson.convertTo[Menu]
      }
}

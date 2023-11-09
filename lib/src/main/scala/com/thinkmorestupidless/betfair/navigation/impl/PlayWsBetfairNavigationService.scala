package com.thinkmorestupidless.betfair.navigation.impl

import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.betfair.navigation.domain.{BetfairNavigationService, Menu}
import org.apache.pekko.actor.ActorSystem
import org.slf4j.LoggerFactory
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import spray.json._

import scala.concurrent.Future

final class PlayWsBetfairNavigationService(config: BetfairConfig)(implicit system: ActorSystem)
    extends BetfairNavigationService
    with JsonCodecs {

  private implicit val ec = system.dispatcher
  private val wsClient = StandaloneAhcWSClient()

  private val log = LoggerFactory.getLogger(getClass)

  override def menu()(implicit session: BetfairSession): Future[Menu] = {
    log.info(s"calling /menu at ${config.navigation.uri.value}")
    wsClient
      .url(config.navigation.uri.value)
      .addHttpHeaders(
        (config.headerKeys.applicationKey.value, session.applicationKey.value),
        (config.headerKeys.sessionToken.value, session.sessionToken.value)
      )
      .get()
      .map { response =>
        try {
          response.body.parseJson.convertTo[Menu]
        } catch {
          case e: Exception => log.error("failed to parse menu")
            Menu(List.empty)
        }
      }
  }
}

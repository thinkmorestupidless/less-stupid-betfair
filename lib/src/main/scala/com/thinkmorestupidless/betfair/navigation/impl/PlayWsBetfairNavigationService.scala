package com.thinkmorestupidless.betfair.navigation.impl

import cats.data.EitherT
import cats.syntax.either._
import com.thinkmorestupidless.betfair.auth.domain.{BetfairAuthenticationService, SessionToken}
import com.thinkmorestupidless.betfair.core.impl.{BetfairConfig, MenuUri, SessionTokenHeaderKey}
import com.thinkmorestupidless.betfair.navigation.domain.BetfairNavigationService.{
  FailedAuthentication,
  NavigationServiceError,
  UnexpectedParsingError
}
import com.thinkmorestupidless.betfair.navigation.domain.{BetfairNavigationService, Menu}
import com.thinkmorestupidless.betfair.navigation.impl.PlayWsBetfairNavigationService.AuthHeader
import org.apache.pekko.actor.ActorSystem
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import sbt.io.{IO => sbtio}
import sbt.io.syntax._
import spray.json._

import scala.concurrent.Future

final class PlayWsBetfairNavigationService private (
    authenticationService: BetfairAuthenticationService,
    menuUri: MenuUri,
    authHeader: AuthHeader,
    sessionTokenHeaderKey: SessionTokenHeaderKey
)(implicit system: ActorSystem)
    extends BetfairNavigationService
    with JsonCodecs {

  private implicit val ec = system.dispatcher

  private val wsClient = StandaloneAhcWSClient()

  override def menu(): EitherT[Future, NavigationServiceError, Menu] =
    withSessionToken(_menu(_))

  private def _menu(sessionToken: SessionToken): EitherT[Future, NavigationServiceError, Menu] =
    EitherT(
      wsClient
        .url(menuUri.value)
        .addHttpHeaders(
          (authHeader.key, authHeader.value),
          (sessionTokenHeaderKey.value, sessionToken.value)
        )
        .get()
        .map { response =>
          sbtio.write(file("menu.json"), response.body)
          try
            response.body.parseJson.convertTo[Menu].asRight
          catch {
            case e: Exception =>
              UnexpectedParsingError(e).asLeft
          }
        }
    )

  private def withSessionToken(
      f: SessionToken => EitherT[Future, NavigationServiceError, Menu]
  ): EitherT[Future, NavigationServiceError, Menu] =
    authenticationService.login().leftMap(FailedAuthentication(_)).flatMap(f(_))
}

object PlayWsBetfairNavigationService {

  final case class AuthHeader(key: String, value: String)

  def apply(config: BetfairConfig, authenticationService: BetfairAuthenticationService)(implicit
      system: ActorSystem
  ): PlayWsBetfairNavigationService = {
    val menuUri = config.navigation.uri
    val authHeader = AuthHeader(config.headerKeys.applicationKey.value, config.auth.credentials.applicationKey.value)
    val sessionTokenHeaderKey = config.headerKeys.sessionToken

    new PlayWsBetfairNavigationService(authenticationService, menuUri, authHeader, sessionTokenHeaderKey)
  }
}

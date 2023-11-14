package com.thinkmorestupidless.betfair.auth.impl

import cats.data.EitherT
import cats.syntax.either._
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.{
  AuthenticationError,
  FailedToDecodeLoginResponseJson,
  FailedToParseLoginResponseAsJson,
  LoginRejectedByBetfair,
  UnexpectedLoginError
}
import com.thinkmorestupidless.betfair.auth.domain._
import com.thinkmorestupidless.betfair.auth.impl.JsonCodecs._
import com.thinkmorestupidless.betfair.auth.impl.PlayWsBetfairAuthenticationService.AuthHeader
import com.thinkmorestupidless.betfair.core.impl.{BetfairConfig, LoginUri}
import io.circe.Json
import io.circe.parser.parse
import org.apache.pekko.actor.ActorSystem
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.Future

final class PlayWsBetfairAuthenticationService private (
    loginUri: LoginUri,
    username: Username,
    password: Password,
    authHeader: AuthHeader
)(implicit system: ActorSystem)
    extends BetfairAuthenticationService {

  private val wsClient = StandaloneAhcWSClient()

  private implicit val ec = system.dispatcher

  override def login(): EitherT[Future, AuthenticationError, SessionToken] =
    EitherT(
      wsClient
        .url(loginUri.value)
        .addHttpHeaders(authHeader.key -> authHeader.value)
        .post(
          Map(
            "username" -> username.value,
            "password" -> password.value
          )
        )
        .map(response => parseResponseString(response.body))
        .recover { case e: Throwable =>
          Left(UnexpectedLoginError(e.getMessage()))
        }
    )

  private def parseResponseString(bodyAsString: String): Either[AuthenticationError, SessionToken] =
    parse(bodyAsString) match {
      case Right(json) => decodeJsonResponse(json)
      case Left(error) => FailedToParseLoginResponseAsJson(bodyAsString, error.getMessage()).asLeft
    }

  private def decodeJsonResponse(json: Json): Either[AuthenticationError, SessionToken] =
    json.as[LoginResponse] match {
      case Right(loginResponse) => matchLoginResponse(loginResponse)
      case Left(error)          => FailedToDecodeLoginResponseJson(json, error).asLeft
    }

  private def matchLoginResponse(loginResponse: LoginResponse): Either[AuthenticationError, SessionToken] =
    loginResponse match {
      case LoginSuccess(sessionToken) => sessionToken.asRight
      case LoginFailure(loginStatus)  => LoginRejectedByBetfair(loginStatus).asLeft
    }
}

object PlayWsBetfairAuthenticationService {

  final case class AuthHeader(key: String, value: String)

  def apply(config: BetfairConfig)(implicit system: ActorSystem): BetfairAuthenticationService = {
    val loginUri = config.auth.uri
    val username = config.auth.credentials.username
    val password = config.auth.credentials.password
    val authHeader = AuthHeader(config.headerKeys.applicationKey.value, config.auth.credentials.applicationKey.value)

    new PlayWsBetfairAuthenticationService(loginUri, username, password, authHeader)
  }
}

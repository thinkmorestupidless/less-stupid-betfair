package com.thinkmorestupidless.betfair.auth.impl

import cats.data.EitherT
import cats.syntax.either._
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.{
  FailedToDecodeLoginResponseJson,
  FailedToParseLoginResponseAsJson,
  LoginError,
  LoginRejectedByBetfair,
  UnexpectedLoginError
}
import com.thinkmorestupidless.betfair.auth.domain._
import com.thinkmorestupidless.betfair.auth.impl.JsonCodecs._
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import io.circe.Json
import io.circe.parser.parse
import org.apache.pekko.actor.ActorSystem
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.Future

class PlayWsBetfairAuthenticationService(config: BetfairConfig)(implicit system: ActorSystem)
    extends BetfairAuthenticationService {

  private val wsClient = StandaloneAhcWSClient()

  private implicit val ec = system.dispatcher

  override def login(): EitherT[Future, LoginError, SessionToken] =
    EitherT(
      wsClient
        .url(config.login.uri.value)
        .addHttpHeaders(config.headerKeys.applicationKey.value -> config.login.credentials.applicationKey.value)
        .post(
          Map(
            "username" -> config.login.credentials.username.value,
            "password" -> config.login.credentials.password.value
          )
        )
        .map(response => parseResponseString(response.body))
        .recover { case e: Throwable =>
          Left(UnexpectedLoginError(e.getMessage()))
        }
    )

  private def parseResponseString(bodyAsString: String): Either[LoginError, SessionToken] =
    parse(bodyAsString) match {
      case Right(json) => decodeJsonResponse(json)
      case Left(error) => FailedToParseLoginResponseAsJson(bodyAsString, error.getMessage()).asLeft
    }

  private def decodeJsonResponse(json: Json): Either[LoginError, SessionToken] =
    json.as[LoginResponse] match {
      case Right(loginResponse) => matchLoginResponse(loginResponse)
      case Left(error)          => FailedToDecodeLoginResponseJson(json, error).asLeft
    }

  private def matchLoginResponse(loginResponse: LoginResponse): Either[LoginError, SessionToken] =
    loginResponse match {
      case LoginSuccess(sessionToken) => sessionToken.asRight
      case LoginFailure(loginStatus)  => LoginRejectedByBetfair(loginStatus).asLeft
    }
}

package com.thinkmorestupidless.betfair.auth.domain

import cats.data.EitherT
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.AuthenticationError
import com.thinkmorestupidless.utils.ValidationException
import io.circe.{DecodingFailure, Json}

import scala.concurrent.Future

trait BetfairAuthenticationService {

  def login(): EitherT[Future, AuthenticationError, SessionToken]
}

object BetfairAuthenticationService {

  sealed trait AuthenticationError {
    def toValidationException(): ValidationException =
      AuthenticationError.toValidationException(this)
  }
  object AuthenticationError {
    def toValidationException(error: AuthenticationError): ValidationException =
      error match {
        case FailedToParseLoginResponseAsJson(body, cause) =>
          ValidationException(s"Failed to parse string as json '$body'", Some(cause))
        case FailedToDecodeLoginResponseJson(json, cause) =>
          ValidationException(s"Failed to decode JSON '${json.spaces2}'", Some(cause))
        case LoginRejectedByBetfair(loginStatus) => ValidationException(s"Login rejected by Betfair '$loginStatus'")
        case UnexpectedLoginError(cause) => ValidationException("Unexpected error returned by Betfair", Some(cause))
      }
  }
  case class FailedToParseLoginResponseAsJson(body: String, cause: Throwable) extends AuthenticationError
  case class FailedToDecodeLoginResponseJson(json: Json, cause: DecodingFailure) extends AuthenticationError
  case class LoginRejectedByBetfair(loginStatus: LoginStatus) extends AuthenticationError
  case class UnexpectedLoginError(cause: Throwable) extends AuthenticationError
}

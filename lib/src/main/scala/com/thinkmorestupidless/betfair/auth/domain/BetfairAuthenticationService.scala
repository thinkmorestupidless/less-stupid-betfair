package com.thinkmorestupidless.betfair.auth.domain

import cats.data.EitherT
import io.circe.{DecodingFailure, Json}
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.AuthenticationError

import scala.concurrent.Future

trait BetfairAuthenticationService {

  def login(): EitherT[Future, AuthenticationError, SessionToken]
}

object BetfairAuthenticationService {

  sealed trait AuthenticationError
  case class FailedToParseLoginResponseAsJson(body: String, cause: String) extends AuthenticationError
  case class FailedToDecodeLoginResponseJson(json: Json, cause: DecodingFailure) extends AuthenticationError
  case class LoginRejectedByBetfair(loginStatus: LoginStatus) extends AuthenticationError
  case class UnexpectedLoginError(cause: String) extends AuthenticationError
}

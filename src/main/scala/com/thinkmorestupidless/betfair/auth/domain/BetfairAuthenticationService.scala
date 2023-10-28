package com.thinkmorestupidless.betfair.auth.domain

import cats.data.EitherT
import io.circe.{DecodingFailure, Json}
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.LoginError

import scala.concurrent.Future

trait BetfairAuthenticationService {

  def login(): EitherT[Future, LoginError, SessionToken]
}

object BetfairAuthenticationService {

  sealed trait LoginError
  case class FailedToParseLoginResponseAsJson(body: String, cause: String) extends LoginError
  case class FailedToDecodeLoginResponseJson(json: Json, cause: DecodingFailure) extends LoginError
  case class LoginRejectedByBetfair(loginStatus: LoginStatus) extends LoginError
  case class UnexpectedLoginError(cause: String) extends LoginError
}

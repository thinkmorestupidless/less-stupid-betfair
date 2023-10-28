package com.thinkmorestupidless.betfair.auth.domain

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.{CirceEnum, Enum, EnumEntry}

final case class BetfairCredentials(username: Username, password: Password, applicationKey: ApplicationKey)
final case class Username(value: String)
final case class Password(value: String)
final case class ApplicationKey(value: String)
final case class SessionToken(value: String)

case class BetfairSession(applicationKey: ApplicationKey, sessionToken: SessionToken) {

  def withSessionToken(sessionToken: SessionToken): BetfairSession =
    copy(sessionToken = sessionToken)
}

sealed trait LoginStatus extends EnumEntry with UpperSnakecase
object LoginStatus extends Enum[LoginStatus] with CirceEnum[LoginStatus] {
  val values = findValues

  case object None extends LoginStatus
  case object Success extends LoginStatus
  case object Error extends LoginStatus
  case object CertAuthRequired extends LoginStatus
  case object BettingRestrictedLocation extends LoginStatus
  case object AccountPendingPasswordChange extends LoginStatus
}

sealed trait LoginResponse
final case class LoginSuccess(sessionToken: SessionToken) extends LoginResponse
final case class LoginFailure(loginStatus: LoginStatus) extends LoginResponse

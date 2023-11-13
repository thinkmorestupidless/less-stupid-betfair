package com.thinkmorestupidless.betfair.vendor.domain

import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.vendor.domain.BetfairVendorApi.TokenResponse

import scala.concurrent.Future

trait BetfairVendorApi {

  def exchangeCodeForToken(code: Code)(implicit session: BetfairSession): Future[TokenResponse]

  def refreshToken(refreshToken: RefreshToken)(implicit session: BetfairSession): Future[TokenResponse]
}

object BetfairVendorApi {

  case object FailedToExchangeCodeForToken

  final case class TokenResponse(
      accessToken: AccessToken,
      tokenType: TokenType,
      expiresIn: ExpiresIn,
      refreshToken: RefreshToken,
      applicationSubscription: ApplicationSubscription
  )
}

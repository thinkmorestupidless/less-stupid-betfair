package com.thinkmorestupidless.betfair.vendor.impl

import com.thinkmorestupidless.betfair.vendor.domain.BetfairVendorApi.TokenResponse
import com.thinkmorestupidless.betfair.vendor.domain.{
  AccessToken,
  ApplicationSubscription,
  ClientId,
  ClientSecret,
  Code,
  ExpiresIn,
  GrantType,
  RefreshToken,
  TokenType
}
import com.thinkmorestupidless.betfair.vendor.impl.AkkaHttpBetfairVendorApi.{RefreshTokenRequest, TokenExchangeRequest}
import com.thinkmorestupidless.extensions.circe.CirceUtils._
import io.circe.Codec
import io.circe._
import io.circe.generic.semiauto._

object JsonCodecs {

  implicit val clientIdCodec: Codec[ClientId] = bimapString(_.value, ClientId(_))
  implicit val codeCodec: Codec[Code] = bimapString(_.value, Code(_))
  implicit val clientSecretCodec: Codec[ClientSecret] = bimapString(_.value, ClientSecret(_))
  implicit val accessTokenCodec: Codec[AccessToken] = bimapString(_.value, AccessToken(_))
  implicit val tokenTypeCodec: Codec[TokenType] = bimapString(_.value, TokenType(_))
  implicit val expiresInCodec: Codec[ExpiresIn] = bimapLong(_.value, ExpiresIn(_))
  implicit val refreshToken: Codec[RefreshToken] = bimapString(_.value, RefreshToken(_))
  implicit val applicationSubscriptionCodec: Codec[ApplicationSubscription] =
    bimapString(_.value, ApplicationSubscription(_))

  implicit val tokenExchangeRequestCodec: Codec[TokenExchangeRequest] = deriveCodec
  implicit val refreshTokenRequestCodec: Codec[RefreshTokenRequest] = deriveCodec

  implicit val tokenResponseCodec: Codec[TokenResponse] = deriveCodec
}

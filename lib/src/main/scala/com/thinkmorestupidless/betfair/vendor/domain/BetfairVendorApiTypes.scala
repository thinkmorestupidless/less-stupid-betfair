package com.thinkmorestupidless.betfair.vendor.domain

import enumeratum.{CirceEnum, Enum, EnumEntry}
import enumeratum.EnumEntry.UpperSnakecase

sealed trait GrantType extends EnumEntry with UpperSnakecase
object GrantType extends Enum[GrantType] with CirceEnum[GrantType] {
  override def values: IndexedSeq[GrantType] = findValues
  object AuthorizationCode extends GrantType
  object RefreshToken extends GrantType
}
final case class Code(value: String)
final case class AccessToken(value: String)
final case class TokenType(value: String)
final case class ExpiresIn(value: Long)
final case class RefreshToken(value: String)
final case class ApplicationSubscription(value: String)

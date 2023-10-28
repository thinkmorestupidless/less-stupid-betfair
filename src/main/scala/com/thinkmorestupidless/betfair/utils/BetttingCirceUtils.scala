package com.thinkmorestupidless.betfair.utils

import com.thinkmorestupidless.betfair.core._
import com.thinkmorestupidless.betfair.core.domain.{Money, Price}
import com.thinkmorestupidless.utils.CirceUtils._
import io.circe.{Codec, Decoder, Encoder}

object BettingCirceUtils {

  implicit val moneyCodec: Codec[Money] = bimapDecimal(_.amount, Money(_))
  implicit val priceCodec: Codec[Price] = bimapDecimal(_.value, Price(_))

  def bimapPrice[T](asPrice: T => Price, fromPrice: Price => T): Codec[T] =
    Codec.from(
      Decoder.decodeBigDecimal.map(Price(_)).map(fromPrice),
      Encoder.encodeBigDecimal.contramap[T](asPrice.andThen(_.value))
    )

  def bimapMoney[T](asMoney: T => Money, fromMoney: Money => T): Codec[T] =
    Codec.from(
      Decoder.decodeBigDecimal.map(Money(_)).map(fromMoney),
      Encoder.encodeBigDecimal.contramap[T](asMoney.andThen(_.amount))
    )
}

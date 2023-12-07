package com.thinkmorestupidless.betfair.core.impl

import com.thinkmorestupidless.betfair.core.domain.{Money, Price}
import com.thinkmorestupidless.extensions.circe.CirceUtils.bimapDecimal
import io.circe.Codec

object JsonCodecs {

  implicit val moneyCodec: Codec[Money] = bimapDecimal(_.amount, Money(_))
  implicit val priceCoded: Codec[Price] = bimapDecimal(_.value, Price(_))
}

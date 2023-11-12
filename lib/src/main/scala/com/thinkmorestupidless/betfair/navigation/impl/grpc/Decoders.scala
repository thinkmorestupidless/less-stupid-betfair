package com.thinkmorestupidless.betfair.navigation.impl.grpc

import cats.data.Validated
import com.thinkmorestupidless.betfair.navigation.domain.GetMenuRequest
import com.thinkmorestupidless.betfair.proto.navigation.{GetMenuRequest => GetMenuRequestProto}
import com.thinkmorestupidless.grpc.Decoder

object Decoders {

  implicit val getMenuRequestDecoder: Decoder[GetMenuRequestProto, GetMenuRequest] =
    _ => Validated.valid(GetMenuRequest())
}

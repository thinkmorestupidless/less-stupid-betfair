package com.thinkmorestupidless.betfair.navigation.impl.grpc

import cats.syntax.validated._
import com.thinkmorestupidless.betfair.navigation.domain.GetMenuRequest
import com.thinkmorestupidless.betfair.proto.navigation.{GetMenuRequest => GetMenuRequestProto}
import com.thinkmorestupidless.grpc.Decoder

object Decoders {

  implicit val getMenuRequestDecoder: Decoder[GetMenuRequestProto, GetMenuRequest] =
    _ => GetMenuRequest().validNel
}

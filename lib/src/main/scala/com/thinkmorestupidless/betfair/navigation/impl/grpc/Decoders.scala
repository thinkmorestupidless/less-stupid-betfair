package com.thinkmorestupidless.betfair.navigation.impl.grpc

import cats.data.Validated
import com.thinkmorestupidless.betfair.navigation.domain.GetMenuRequest
import com.thinkmorestupidless.betfair.proto.navigation.{GetMenuRequest => GetMenuRequestProto}
import com.thinkmorestupidless.grpc.Decoder
import com.thinkmorestupidless.utils.ValidationException

object Decoders {

  implicit val getMenuRequestDecoder: Decoder[ValidationException, GetMenuRequestProto, GetMenuRequest] =
    _ => Validated.valid(GetMenuRequest())
}

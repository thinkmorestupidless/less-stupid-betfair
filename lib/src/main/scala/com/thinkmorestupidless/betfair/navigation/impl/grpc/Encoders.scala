package com.thinkmorestupidless.betfair.navigation.impl.grpc

import com.thinkmorestupidless.betfair.proto.navigation.{Menu => MenuProto}
import com.thinkmorestupidless.betfair.navigation.domain.Menu
import com.thinkmorestupidless.grpc.Encoder

object Encoders {

  implicit val menuEncoder: Encoder[Menu, MenuProto] =
    menu => MenuProto.defaultInstance
}

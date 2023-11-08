package com.thinkmorestupidless.grpc

import cats.data.Validated
import scalapb.GeneratedMessage

trait Decoder[ERROR, IN <: GeneratedMessage, OUT] extends (IN => Validated[ERROR, OUT])

object Decoder {

  implicit class DecoderOps[IN <: GeneratedMessage](request: IN) {
    def decode[ERROR, OUT](implicit decoder: Decoder[ERROR, IN, OUT]): Validated[ERROR, OUT] =
      decoder(request)
  }
}

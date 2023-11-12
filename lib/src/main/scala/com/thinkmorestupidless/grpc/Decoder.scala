package com.thinkmorestupidless.grpc

import com.thinkmorestupidless.utils.Validation.Validation
import scalapb.GeneratedMessage

trait Decoder[IN <: GeneratedMessage, OUT] extends (IN => Validation[OUT])

object Decoder {

  implicit class DecoderOps[IN <: GeneratedMessage](request: IN) {
    def decode[OUT](implicit decoder: Decoder[IN, OUT]): Validation[OUT] =
      decoder(request)
  }
}

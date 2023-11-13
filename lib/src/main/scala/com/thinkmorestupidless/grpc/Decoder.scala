package com.thinkmorestupidless.grpc

import com.thinkmorestupidless.utils.Validation.Validation

trait Decoder[IN, OUT] extends (IN => Validation[OUT])

object Decoder {

  implicit class DecoderOps[IN](request: IN) {
    def decode[OUT](implicit decoder: Decoder[IN, OUT]): Validation[OUT] =
      decoder(request)
  }
}

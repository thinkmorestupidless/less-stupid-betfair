package com.thinkmorestupidless.grpc

import scalapb.GeneratedMessage

trait Encoder[IN, OUT <: GeneratedMessage] extends (IN => OUT)

object Encoder {

  implicit class EncoderOps[IN](self: IN) {

    def encode[OUT <: GeneratedMessage](implicit encoder: Encoder[IN, OUT]): OUT =
      encoder(self)
  }
}

package com.thinkmorestupidless.grpc

trait Encoder[IN, OUT] extends (IN => OUT)

object Encoder {

  implicit class EncoderOps[IN](self: IN) {

    def encode[OUT](implicit encoder: Encoder[IN, OUT]): OUT =
      encoder(self)
  }
}

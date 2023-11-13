package com.thinkmorestupidless.grpc

import cats.data.{NonEmptyList, Validated}
import com.thinkmorestupidless.utils.Validation.Validation
import com.thinkmorestupidless.utils.ValidationException

trait Decoder[IN, OUT] extends (IN => Validation[OUT])

object Decoder {

  implicit class DecoderOps[IN](request: IN) {
    def decode[OUT](implicit decoder: Decoder[IN, OUT]): Validation[OUT] =
      decoder(request)
  }

  def ensureOptionIsDefined[T](from: Option[T], message: String): Validation[T] =
    Validated.cond(from.isDefined, from.get, NonEmptyList.one(ValidationException(message)))
}

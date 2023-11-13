package com.thinkmorestupidless.grpc

import com.thinkmorestupidless.grpc.Decoder._
import cats.syntax.traverse._
import cats.syntax.validated._
import com.thinkmorestupidless.utils.Validation.ImplicitConversions.{
  validatedOptionalStringToValidatedOptionalBigDecimal,
  validatedStringToValidatedBigDecimal
}
import com.thinkmorestupidless.utils.Validation.Validation

import scala.language.implicitConversions

object DefaultDecoders {

  def validNone[T]: Validation[Option[T]] = None.validNel

  implicit def optionalA_optionalB[A, B](implicit decoder: Decoder[A, B]): Decoder[Option[A], Option[B]] =
    _.map(_.decode).sequence

  implicit val optionalString_optionalBigDecimal: Decoder[Option[String], Option[BigDecimal]] = _.validNel
  implicit val optionalString_optionalString: Decoder[Option[String], Option[String]] = _.validNel
  implicit val string_string: Decoder[String, String] = _.validNel
  implicit val seqString_listString: Decoder[Seq[String], List[String]] = _.toList.validNel
  implicit val int_int: Decoder[Int, Int] = _.validNel
  implicit val long_long: Decoder[Long, Long] = _.validNel
  implicit val string_bigDecimal: Decoder[String, BigDecimal] = _.validNel
  implicit val boolean_boolean: Decoder[Boolean, Boolean] = _.validNel
}

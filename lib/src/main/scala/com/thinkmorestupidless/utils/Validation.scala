package com.thinkmorestupidless.utils

import cats.Applicative
import cats.data.{EitherT, NonEmptyList, Validated}

import scala.util.{Failure, Success, Try}

final case class ValidationException(message: String, cause: Option[Throwable] = None)
    extends RuntimeException(message, cause.orNull)

object ValidationException {
  def combineErrors(multipleErrors: NonEmptyList[ValidationException]): ValidationException =
    ValidationException(combinedMessage(multipleErrors))

  private def combinedMessage(errors: NonEmptyList[ValidationException]): String = errors.toList.mkString(",")
}

object Validation {
  type Validation[T] = Validated[NonEmptyList[ValidationException], T]

  implicit class ValidationOps[T](self: Validated[NonEmptyList[ValidationException], T]) {
    def combined: Validated[ValidationException, T] = self.leftMap(ValidationException.combineErrors)
    def toEitherCombined: Either[ValidationException, T] = combined.toEither
    def toEitherTCombined[F[_]: Applicative]: EitherT[F, ValidationException, T] = EitherT.fromEither(toEitherCombined)
    def toTryCombined: Try[T] = toEitherCombined.toTry
  }

  object ImplicitConversions {
    import cats.syntax.validated._

    implicit def toValidatedOptionalList[T](input: Validation[List[T]]): Validation[Option[List[T]]] =
      input.map(_ match {
        case Nil  => None
        case list => Some(list)
      })

    implicit def validatedStringToValidatedBigDecimal(input: Validation[String]): Validation[BigDecimal] =
      Try(input.map(BigDecimal(_))) match {
        case Success(result) => result
        case Failure(error) =>
          ValidationException(s"failed to convert Option[String] to Option[BigDecimal]", Some(error)).invalidNel
      }

    implicit def validatedOptionalStringToValidatedOptionalBigDecimal(
        input: Validation[Option[String]]
    ): Validation[Option[BigDecimal]] =
      Try(input.map(_.map(BigDecimal(_)))) match {
        case Success(result) => result
        case Failure(error) =>
          ValidationException(s"failed to convert Option[String] to Option[BigDecimal]", Some(error)).invalidNel
      }
  }
}

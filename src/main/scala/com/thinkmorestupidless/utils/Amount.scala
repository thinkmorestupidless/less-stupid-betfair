package com.thinkmorestupidless.utils

import cats.data.Validated
import com.thinkmorestupidless.utils.PositiveAmount.unsafe
import com.thinkmorestupidless.utils.Validation.{Validation, ValidationOps}

import scala.math.Ordered.orderingToOrdered

final case class PositiveAmount[T] private (value: T) {
  def map[R: Zero: Ordering](fn: T => R): PositiveAmount[R] = unsafe(fn(value))
}

object PositiveAmount {
  def ensure[T: Zero: Ordering](value: T): Validation[PositiveAmount[T]] =
    Validated.condNel(
      value > Zero[T].get,
      PositiveAmount(value),
      ValidationException(s"Expected positive value, got $value")
    )

  def unsafe[T: Zero: Ordering](value: T): PositiveAmount[T] = ensure(value).toTryCombined.get
}

trait Zero[T] {
  def get: T
}
object Zero {
  def apply[T](implicit ev: Zero[T]): Zero[T] = ev
  def instance[T](value: T): Zero[T] =
    new Zero[T] {
      override def get: T = value
    }
}

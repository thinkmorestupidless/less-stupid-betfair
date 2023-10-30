package com.thinkmorestupidless.betfair.core.domain

import cats.kernel.Monoid
import com.thinkmorestupidless.utils.Zero

import scala.math.BigDecimal.RoundingMode

case class Money(amount: BigDecimal) {
  def +(other: Money): Money = Money(this.amount + other.amount)
  def -(other: Money): Money = Money(this.amount - other.amount)
  def *(n: BigDecimal): Money = Money(this.amount * n)
}

object Money {
  implicit val ordering: Ordering[Money] = Ordering.by(_.amount)
  implicit val zero: Zero[Money] = Zero.instance(Money(0))
  implicit val monoid: Monoid[Money] = Monoid.instance(zero.get, _ + _)

  def apply(amount: BigDecimal): Money = new Money(amount.setScale(2, RoundingMode.HALF_UP))
}

package com.thinkmorestupidless.betfair.core.domain

import scala.math.BigDecimal.RoundingMode

case class Price(value: BigDecimal) {
  require(
    value >= Price.MinValue && value <= Price.MaxValue,
    s"Invalid decimal odds value $value, must be [${Price.MinValue}, ${Price.MaxValue})"
  )

  def *(number: BigDecimal): Price =
    Price(((value - 1) * number) + 1)

  def /(number: BigDecimal): Price =
    Price(((value - 1) / number) + 1)

  def <>(other: Price): BigDecimal =
    1 - ((other.value - 1) / (value - 1))

  def <%>(other: Price): BigDecimal =
    (this <> other) * 100
}

object Price {
  private val precision = 4
  val MinValue: BigDecimal = scale(BigDecimal(1.01))
  val MaxValue: BigDecimal = scale(BigDecimal(1000))

  implicit val ordering: Ordering[Price] = Ordering.by(_.value)

  def apply(value: BigDecimal): Price = new Price(scale(value))

  def apply(value: Double): Price = apply(BigDecimal(value))

  private def scale(value: BigDecimal): BigDecimal = value.setScale(precision, RoundingMode.HALF_UP)
}

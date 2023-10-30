package com.thinkmorestupidless.utils

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum._

sealed trait Direction extends EnumEntry with UpperSnakecase

object Direction extends Enum[Direction] {
  val values = findValues

  case object Ascending extends Direction
  case object Descending extends Direction
}

package com.thinkmorestupidless.utils

import java.time.Clock
import scala.util.Random

object RandomUtils {

  def generateRandomString(clock: Clock = Clock.systemUTC()): String =
    s"${clock.millis()}_${Random.alphanumeric.take(6).mkString}"
}

package com.thinkmorestupidless.betfair.streams.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import io.circe.parser._
import com.thinkmorestupidless.betfair.streams.impl.JsonCodecs._

final class LevelBasedPriceLadderSpec extends AnyWordSpecLike with Matchers {

  "from json" should {
    "work" in {
      val str =
        "[[1,1.2,30670.05],[0,1.19,6158.22],[4,1.23,48.02],[2,1.21,388.81],[3,1.22,13.11],[5,1.24,31.15],[6,1.25,30.78],[7,980,0.31],[8,0,0],[9,0,0]]"
      val json = parse(str).getOrElse(fail("failed to parse price ladder"))
      val priceLadder = json.as[LevelBasedPriceLadder]
    }
  }
}

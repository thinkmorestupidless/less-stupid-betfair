package com.thinkmorestupidless.betfair.streams.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import io.circe.parser._
import com.thinkmorestupidless.betfair.streams.impl.JsonCodecs._

final class PricePointPriceLadderSpec extends AnyWordSpecLike with Matchers {

  "from json" should {
    "parse correctly from json" ignore {
      val str =
        "[[3.25,1441.4],[3.4,1285.24],[3.35,2006.31],[3.3,1108.37],[3.5,85.46],[3.45,436.47],[3.7,29.05],[1000,7.35],[3.6,490],[400,5],[980,0.05],[4.9,15.88],[10,9.99]]"
      val json = parse(str).getOrElse(fail("failed to parse json"))
      val priceLadder = json.as[PricePointPriceLadder]

    }
  }
}

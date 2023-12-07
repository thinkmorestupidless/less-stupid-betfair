package com.thinkmorestupidless.betfair.streams.domain

import io.circe.parser.parse
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.thinkmorestupidless.betfair.streams.impl.JsonCodecs._

final class RunnerChangeSpec extends AnyWordSpecLike with Matchers {

  "from json" should {
    "parse correctly when a field is missing" in {
      val str =
        """{"atb":[[1.04,6859.3],[1.05,5144.47],[1.06,4287.06],[1.02,12863.18],[1.03,8574.12],[1.01,25881.37],[2.42,8.13]],"atl":[[2.7,24.41]],"batb":[[6,1.01,25881.37],[5,1.02,12863.18],[4,1.03,8574.12],[3,1.04,6859.3],[2,1.05,5144.47],[1,1.06,4287.06],[0,2.42,8.13]],"batl":[[0,2.7,24.41]],"bdatb":[[3,2.36,9.1],[5,2.12,4.43],[6,1.06,4287.06],[7,1.05,5144.47],[8,1.04,6859.3],[9,1.03,8574.12],[4,2.3,8.75],[1,2.4,6.96],[2,2.38,23.01],[0,2.42,8.13]],"bdatl":[[1,2.78,7.73],[2,2.8,25.11],[3,2.82,6.84],[4,2.9,5.56],[0,2.7,24.41],[5,0,0],[6,0,0],[7,0,0],[8,0,0],[9,0,0]],"spn":2.56,"spf":"NaN","id":56343}"""
      val json = parse(str).getOrElse(fail("failed to parse json string with missing field"))
      json.as[RunnerChange].getOrElse(fail("failed to decode Json to RunnerChange"))
    }
  }
}

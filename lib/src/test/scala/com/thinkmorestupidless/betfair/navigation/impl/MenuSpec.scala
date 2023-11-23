package com.thinkmorestupidless.betfair.navigation.impl

import com.thinkmorestupidless.betfair.navigation.domain.Menu
import com.thinkmorestupidless.betfair.navigation.impl.JsonCodecs._
import com.thinkmorestupidless.betfair.navigation.impl.MenuUtils._
import com.thinkmorestupidless.utils.FileSupport.jsonFromResource
import org.scalatest.Inspectors
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class MenuSpec extends AnyWordSpecLike with Matchers with Inspectors {

  "Menu" should {

    "find an event by name" in {

      val menu = jsonFromResource("full_menu.json").convertTo[Menu]

      val expectedEvent = menu.findEventByName(EventNames.EnglishPremierLeague)

      expectedEvent.get.name shouldBe EventNames.EnglishPremierLeague
    }

    "find all markets with given marketType for an Event" in {

      val menu = jsonFromResource("full_menu.json").convertTo[Menu]

      val markets = menu
        .findEventByName(EventNames.EnglishPremierLeague)
        .toList
        .flatMap(_.getMarketsWithMarketType(MarketTypes.MatchOdds))
    }
  }
}

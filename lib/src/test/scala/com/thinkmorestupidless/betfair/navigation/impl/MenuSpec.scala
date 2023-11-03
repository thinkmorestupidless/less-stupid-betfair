package com.thinkmorestupidless.betfair.navigation.impl

import com.thinkmorestupidless.betfair.navigation.domain.EventName.EnglishPremierLeague
import com.thinkmorestupidless.betfair.navigation.domain.MarketType.MatchOdds
import com.thinkmorestupidless.betfair.navigation.domain.{EventName, Menu}
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

      val expectedEvent = menu.findEventByName(EnglishPremierLeague)

      expectedEvent.get.name shouldBe EventName.EnglishPremierLeague
    }

    "find all markets with given marketType for an Event" in {

      val menu = jsonFromResource("full_menu.json").convertTo[Menu]

      val _ = menu.allEvents().ofType(EnglishPremierLeague).allMarkets().ofType(MatchOdds)
    }
  }
}

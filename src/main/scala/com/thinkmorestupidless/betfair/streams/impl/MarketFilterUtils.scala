package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.streams.domain.MarketFilter
import com.thinkmorestupidless.utils.OptionalSetUtils._

object MarketFilterUtils {

  implicit class MarketFilterOps(self: MarketFilter) {
    def diff(other: MarketFilter): MarketFilter =
      MarketFilter(
        marketIds = self.marketIds.diff(other.marketIds),
        bspMarket = self.bspMarket,
        bettingTypes = self.bettingTypes.diff(other.bettingTypes),
        eventTypeIds = self.eventTypeIds.diff(other.eventTypeIds),
        eventIds = self.eventIds.diff(other.eventIds),
        turnInPlayEnabled = self.turnInPlayEnabled,
        marketTypes = self.marketTypes.diff(other.marketTypes),
        venues = self.venues.diff(other.venues),
        countryCodes = self.countryCodes.diff(other.countryCodes),
        raceTypes = self.raceTypes.diff(other.raceTypes)
      )

    def mergeWith(other: MarketFilter): MarketFilter =
      new MarketFilter(
        self.marketIds + other.marketIds,
        other.bspMarket,
        self.bettingTypes + other.bettingTypes,
        self.eventTypeIds + other.eventTypeIds,
        self.eventIds + other.eventIds,
        other.turnInPlayEnabled,
        self.marketTypes + other.marketTypes,
        self.venues + other.venues,
        self.countryCodes + other.countryCodes,
        self.raceTypes + other.raceTypes
      )
  }
}

package com.thinkmorestupidless.betfair.strategy.impl

import com.thinkmorestupidless.betfair.streams.domain.{MarketDefinition, MarketFilter}

object MarketDefinitionUtils {

  implicit class MarketDefinitionOps(self: MarketDefinition) {
    def filter(marketFilter: MarketFilter): Boolean =
      filterEventIds(marketFilter.eventIds)
        .orElse(filterEventTypeIds(marketFilter.eventTypeIds))
        .orElse(filterMarketTypes(marketFilter.marketTypes))
        .orElse(filterVenues(marketFilter.venues))
        .exists(_ => true)

    private def filterEventIds(eventIds: Option[List[String]]): Option[MarketDefinition] =
      eventIds.flatMap(ids => if (ids.contains(self.eventId)) Some(self) else None)

    private def filterEventTypeIds(eventTypeIds: Option[List[String]]): Option[MarketDefinition] =
      eventTypeIds.flatMap(ids => if (ids.contains(self.eventTypeId)) Some(self) else None)

    private def filterMarketTypes(marketTypes: Option[List[String]]): Option[MarketDefinition] =
      marketTypes.flatMap(ids => if (ids.contains(self.marketType)) Some(self) else None)

    private def filterVenues(venues: Option[List[String]]): Option[MarketDefinition] =
      self.venue.flatMap(venue => venues.flatMap(ids => if (ids.contains(venue)) Some(self) else None))
  }
}

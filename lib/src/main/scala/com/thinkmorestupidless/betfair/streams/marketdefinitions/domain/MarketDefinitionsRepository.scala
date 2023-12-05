package com.thinkmorestupidless.betfair.streams.marketdefinitions.domain

import com.thinkmorestupidless.betfair.streams.domain.{MarketDefinition, MarketId}
import org.apache.pekko.Done

import scala.concurrent.Future

trait MarketDefinitionsRepository {
  def updateMarketDefinition(marketId: MarketId, marketDefinition: MarketDefinition): Future[Done]
  def getMarketDefinition(marketId: MarketId): Future[Option[MarketDefinition]]
}

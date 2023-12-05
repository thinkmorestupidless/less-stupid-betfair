package com.thinkmorestupidless.betfair.streams.marketdefinitions.impl

import com.thinkmorestupidless.betfair.streams.domain.{MarketDefinition, MarketId}
import com.thinkmorestupidless.betfair.streams.marketdefinitions.domain.MarketDefinitionsRepository
import org.apache.pekko.Done

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

final class InMemoryMarketDefinitionsRepository()(implicit ec: ExecutionContext) extends MarketDefinitionsRepository {

  private val marketDefinitions: mutable.Map[MarketId, MarketDefinition] = mutable.Map.empty

  override def updateMarketDefinition(marketId: MarketId, marketDefinition: MarketDefinition): Future[Done] =
    Future {
      marketDefinitions.getOrElseUpdate(marketId, marketDefinition)
      Done
    }

  override def getMarketDefinition(marketId: MarketId): Future[Option[MarketDefinition]] =
    Future.successful(marketDefinitions.get(marketId))
}

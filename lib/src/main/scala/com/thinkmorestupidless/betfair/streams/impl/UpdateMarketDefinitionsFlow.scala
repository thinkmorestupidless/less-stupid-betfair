package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.streams.domain.{IncomingBetfairSocketMessage, MarketChange, MarketChangeMessage}
import com.thinkmorestupidless.betfair.streams.marketdefinitions.domain.MarketDefinitionsRepository
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.{Done, NotUsed}

import scala.concurrent.{ExecutionContext, Future}

object UpdateMarketDefinitionsFlow {

  def apply(
      marketDefinitionsRepository: MarketDefinitionsRepository
  )(implicit ec: ExecutionContext): Flow[IncomingBetfairSocketMessage, IncomingBetfairSocketMessage, NotUsed] =
    Flow[IncomingBetfairSocketMessage].mapAsync(parallelism = 1) {
      case message @ MarketChangeMessage(_, _, _, _, _, _, _, marketChanges, _, _, _) =>
        updateMarketDefinitions(marketDefinitionsRepository, marketChanges).map(_ => message)
      case message => Future.successful(message)
    }

  private def updateMarketDefinitions(
      marketDefinitionsRepository: MarketDefinitionsRepository,
      marketChanges: Set[MarketChange]
  )(implicit ec: ExecutionContext): Future[Done] =
    Future
      .sequence(marketChanges.map {
        case MarketChange(_, _, _, _, Some(marketDefinition), id) =>
          marketDefinitionsRepository.updateMarketDefinition(id, marketDefinition)
        case _ => Future.successful(Done)
      })
      .map(_ => Done)
}

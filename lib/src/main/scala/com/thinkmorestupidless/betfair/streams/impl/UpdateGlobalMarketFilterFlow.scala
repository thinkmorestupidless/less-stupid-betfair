package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.streams.domain.{
  GlobalMarketFilterRepository,
  MarketSubscription,
  OutgoingBetfairSocketMessage
}
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow

import scala.concurrent.{ExecutionContext, Future}

object UpdateGlobalMarketFilterFlow {

  def apply(
      globalMarketFilterRepository: GlobalMarketFilterRepository
  )(implicit ec: ExecutionContext): Flow[OutgoingBetfairSocketMessage, OutgoingBetfairSocketMessage, NotUsed] =
    Flow[OutgoingBetfairSocketMessage].mapAsync(parallelism = 1) {
      case message @ MarketSubscription(_, Some(marketFilter)) =>
        globalMarketFilterRepository.upsertGlobalMarketFilter(marketFilter).map(_ => message)
      case message => Future.successful(message)
    }
}

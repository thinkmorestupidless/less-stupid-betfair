package com.thinkmorestupidless.betfair.streams.domain.usecases

import com.thinkmorestupidless.betfair.streams.domain.{MarketChangeMessage, MarketFilter, MarketSubscription}
import com.thinkmorestupidless.betfair.streams.impl.BetfairSocketFlow
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.Source

object SubscribeToMarketChangesUseCase {

  type SubscribeToMarketChangesUseCase = MarketFilter => Source[MarketChangeMessage, NotUsed]

  def apply(betfairSocketFlow: BetfairSocketFlow)(implicit system: ActorSystem[_]): SubscribeToMarketChangesUseCase =
    marketFilter => {
      val request = MarketSubscription(marketFilter)
      val (sink, source) = betfairSocketFlow.sinkAndSource()
      Source.single(request).runWith(sink)
      source.collect { case marketChangeMessage: MarketChangeMessage =>
        marketChangeMessage
      }
    }
}

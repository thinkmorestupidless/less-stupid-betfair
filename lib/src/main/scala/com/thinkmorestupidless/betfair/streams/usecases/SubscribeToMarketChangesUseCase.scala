package com.thinkmorestupidless.betfair.streams.usecases

import com.thinkmorestupidless.betfair.streams.domain.{MarketChangeMessage, MarketFilter, MarketSubscription}
import com.thinkmorestupidless.betfair.streams.impl.BetfairSocket
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.Source

object SubscribeToMarketChangesUseCase {

  type SubscribeToMarketChangesUseCase = MarketFilter => Source[MarketChangeMessage, NotUsed]

  def apply(betfairSocket: BetfairSocket)(implicit system: ActorSystem[_]): SubscribeToMarketChangesUseCase =
    marketFilter => {
      val request = MarketSubscription(marketFilter)
      Source.single(request).runWith(betfairSocket.sink)
      betfairSocket.source.collect { case marketChangeMessage: MarketChangeMessage =>
        marketChangeMessage
      }
    }
}

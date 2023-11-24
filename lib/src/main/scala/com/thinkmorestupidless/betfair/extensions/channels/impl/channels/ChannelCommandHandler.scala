package com.thinkmorestupidless.betfair.extensions.channels.impl.channels

import com.thinkmorestupidless.betfair.extensions.channels.impl.channels.ChannelProtocol.{
  Command,
  GetMarketFilter,
  UpdateMarketFilter
}
import com.thinkmorestupidless.betfair.streams.domain.MarketFilter
import com.thinkmorestupidless.betfair.streams.impl.MarketFilterUtils._
import org.apache.pekko.Done
import org.apache.pekko.persistence.typed.state.scaladsl.Effect

object ChannelCommandHandler {

  def apply(): (MarketFilter, Command) => Effect[MarketFilter] =
    (marketFilter, command) =>
      command match {
        case UpdateMarketFilter(newMarketFilter, replyTo) =>
          Effect.persist(marketFilter.mergeWith(newMarketFilter)).thenReply(replyTo)(_ => Done)
        case GetMarketFilter(replyTo) => Effect.reply(replyTo)(marketFilter)
      }
}

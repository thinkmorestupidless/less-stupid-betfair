package com.thinkmorestupidless.betfair.streams.marketdefinitions.impl.cluster

import com.thinkmorestupidless.betfair.streams.domain.MarketDefinition
import com.thinkmorestupidless.betfair.streams.marketdefinitions.impl.cluster.MarketDefinitionProtocol.{
  Command,
  GetMarketDefinition,
  MarketDefinitionNotInitialized,
  UpdateMarketDefinition
}
import org.apache.pekko.Done
import org.apache.pekko.actor.Status
import org.apache.pekko.persistence.typed.state.scaladsl.Effect

object MarketDefinitionCommandHandler {

  def apply(): (Option[MarketDefinition], Command) => Effect[Option[MarketDefinition]] =
    (currentState, command) =>
      (currentState, command) match {
        case (_, UpdateMarketDefinition(marketDefinition, replyTo)) =>
          Effect.persist(Some(marketDefinition)).thenReply(replyTo)(_ => Done)
        case (None, GetMarketDefinition(replyTo)) =>
          Effect.reply(replyTo)(Status.Failure(MarketDefinitionNotInitialized()))
        case (Some(marketDefinition), GetMarketDefinition(replyTo)) =>
          Effect.reply(replyTo)(Status.Success(marketDefinition))
      }
}

package com.thinkmorestupidless.betfair.streams.marketdefinitions.impl.actor

import com.thinkmorestupidless.betfair.streams.domain.{MarketDefinition, MarketId}
import org.apache.pekko.Done
import org.apache.pekko.actor.Status
import org.apache.pekko.actor.Status.Status
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import scala.collection.mutable

object MarketDefinitionsRepositoryBehaviour {

  sealed trait Message
  final case class UpsertMarketDefinition(
      marketId: MarketId,
      marketDefinition: MarketDefinition,
      replyTo: ActorRef[Done]
  ) extends Message
  final case class GetMarketDefinition(marketId: MarketId, replyTo: ActorRef[Status]) extends Message

  def apply(): Behavior[Message] =
    Behaviors.setup { context =>
      def running(marketDefinitions: mutable.Map[MarketId, MarketDefinition]): Behavior[Message] =
        Behaviors.receiveMessage {
          case UpsertMarketDefinition(marketId, marketDefinition, replyTo) =>
            marketDefinitions.getOrElseUpdate(marketId, marketDefinition)
            replyTo ! Done
            Behaviors.same

          case GetMarketDefinition(marketId, replyTo) =>
            val response: Status = marketDefinitions.get(marketId).map(Status.Success(_)).getOrElse(Status.Success(()))
            replyTo ! response
            Behaviors.same
        }

      running(mutable.Map.empty)
    }
}

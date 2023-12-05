package com.thinkmorestupidless.betfair.strategy.impl

import com.thinkmorestupidless.betfair.strategy.domain.BettingStrategy
import com.thinkmorestupidless.betfair.strategy.impl.MarketDefinitionUtils._
import com.thinkmorestupidless.betfair.streams.domain.{MarketChange, MarketDefinition}
import com.thinkmorestupidless.betfair.streams.marketdefinitions.domain.MarketDefinitionsRepository
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

object BettingStrategyOrchestratorBehaviour {

  private val log = LoggerFactory.getLogger(getClass)

  sealed trait Message
  final case class HandleMarketChange(marketChange: MarketChange, replyTo: ActorRef[Done]) extends Message
  final case class MarketDefinitionForMarketChange(marketDefinition: MarketDefinition, marketChange: MarketChange)
      extends Message
  final case class NoMarketDefinitionForMarketChange(marketChange: MarketChange) extends Message
  final case class FailedToGetMarketDefinitionForMarketChange(marketChange: MarketChange, cause: Throwable)
      extends Message
  final case class RegisterBettingStrategy(bettingStrategy: BettingStrategy) extends Message

  def apply(marketDefinitionsRepository: MarketDefinitionsRepository): Behavior[Message] =
    Behaviors.setup { context =>
      def running(bettingStrategies: List[BettingStrategy]): Behavior[Message] =
        Behaviors.receiveMessage {
          case HandleMarketChange(marketChange, replyTo) =>
            log.info(s"handling market change '$marketChange'")
            if (bettingStrategies.nonEmpty) {
              // 1. Get the market definition for this market
              context.pipeToSelf(marketDefinitionsRepository.getMarketDefinition(marketChange.id)) {
                case Success(maybeMarketDefinition) =>
                  maybeMarketDefinition
                    .map(MarketDefinitionForMarketChange(_, marketChange))
                    .getOrElse(NoMarketDefinitionForMarketChange(marketChange))
                case Failure(cause) => FailedToGetMarketDefinitionForMarketChange(marketChange, cause)
              }
            } else {
              log.debug("not retrieving market definition, no betting strategies registered")
            }
            replyTo ! Done
            Behaviors.same

          case MarketDefinitionForMarketChange(marketDefinition, marketChange) =>
            log.info("received market definition for market change")
            // 1. Find betting strategies which care about this market change
            val filteredStrategies =
              bettingStrategies.filter(strategy => marketDefinition.filter(strategy.marketFilter))
            // 2. If there are more than 0 strategies found
            if (filteredStrategies.nonEmpty) {
              // 3. Spawn an actor for each one
              filteredStrategies.map { strategy =>
                log.info(s"spawning child to handle strategy '${strategy.name}'")
                val strategyActor = context.spawn(BettingStrategyBehaviour(strategy.logic()), strategy.name)
                // 4. Send the market change to the child actor
                strategyActor ! BettingStrategyBehaviour.HandleMarketChange(marketChange)
              }
            }
            Behaviors.same

          case NoMarketDefinitionForMarketChange(marketChange) =>
            log.warn(s"unable to find market definition for market change '$marketChange'")
            Behaviors.same

          case FailedToGetMarketDefinitionForMarketChange(marketChange, cause) =>
            log.error(s"failed to retrieve market definition for market change '$marketChange'", cause)
            Behaviors.same

          case RegisterBettingStrategy(bettingStrategy) =>
            log.info(s"registering betting strategy '${bettingStrategy.name}'")
            running(bettingStrategies :+ bettingStrategy)
        }

      running(List.empty)
    }
}

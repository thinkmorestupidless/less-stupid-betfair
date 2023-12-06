package com.thinkmorestupidless.betfair.strategy.impl

import com.thinkmorestupidless.betfair.strategy.domain.BettingStrategyLogic
import com.thinkmorestupidless.betfair.streams.domain.MarketChange
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.util.{Failure, Success}

object BettingStrategyBehaviour {

  sealed trait Message
  final case class HandleMarketChange(marketChange: MarketChange) extends Message
  case object StrategyLogicComplete extends Message
  final case class StrategyLogicFailed(cause: Throwable) extends Message

  def apply(logic: BettingStrategyLogic): Behavior[Message] =
    Behaviors.setup { context =>
      def waitingForTrigger(): Behavior[Message] =
        Behaviors.receiveMessagePartial { case HandleMarketChange(marketChange) =>
          context.pipeToSelf(logic.onMarketChange(marketChange)(context.system.executionContext)) {
            case Success(_)     => StrategyLogicComplete
            case Failure(cause) => StrategyLogicFailed(cause)
          }
          waitingForLogicToComplete()
        }

      def waitingForLogicToComplete(): Behavior[Message] =
        Behaviors.receiveMessagePartial {
          case StrategyLogicComplete =>
            Behaviors.stopped
          case StrategyLogicFailed(cause) =>
            throw cause
        }

      waitingForTrigger()
    }
}

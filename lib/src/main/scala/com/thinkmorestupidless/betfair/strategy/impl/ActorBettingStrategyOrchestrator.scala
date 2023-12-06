package com.thinkmorestupidless.betfair.strategy.impl

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.strategy.domain.{BettingStrategy, BettingStrategyOrchestrator}
import com.thinkmorestupidless.betfair.strategy.impl.BettingStrategyOrchestratorBehaviour.{HandleMarketChange, Message}
import com.thinkmorestupidless.betfair.streams.domain.{
  MarketChange,
  MarketChangeMessage,
  MarketSubscription,
  OutgoingBetfairSocketMessage
}
import com.thinkmorestupidless.betfair.streams.marketdefinitions.domain.MarketDefinitionsRepository
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.stream.QueueOfferResult
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.stream.typed.scaladsl.ActorFlow
import org.apache.pekko.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}

final class ActorBettingStrategyOrchestrator private (betfair: Betfair, actor: ActorRef[Message])(implicit
    system: ActorSystem[_]
) extends BettingStrategyOrchestrator {

  private val log = LoggerFactory.getLogger(getClass)

  implicit private val timeout = Timeout(10.seconds)
  implicit private val ec = system.executionContext

  private val queue = {
    val (queue, source) = Source.queue[OutgoingBetfairSocketMessage](bufferSize = 100).preMaterialize()
    val betfairFlow = betfair.socketFlow
      .flowFromSinkAndSource()
      .collect { case MarketChangeMessage(_, _, _, _, _, _, _, marketChanges, _, _, _) =>
        marketChanges
      }
      .flatMapConcat(Source(_))
      .via(ActorFlow.ask[MarketChange, Message, Done](actor)(HandleMarketChange(_, _)))

    source
      .map { elem =>
        log.info(s"sending message => $elem")
        elem
      }
      .via(betfairFlow)
      .to(Sink.ignore)
      .run()

    queue
  }

  override def onMarketChange(marketChange: MarketChange): Unit =
    actor.ask[Done](replyTo => HandleMarketChange(marketChange, replyTo)).onComplete {
      case Success(_)     => log.debug("handling of market change by strategies has begun")
      case Failure(cause) => log.error("handling of market change by strategies has failed", cause)
    }

  override def registerBettingStrategy(bettingStrategy: BettingStrategy): Unit = {
    val marketSubscription = MarketSubscription(bettingStrategy.marketFilter)
    queue.offer(marketSubscription) match {
      case QueueOfferResult.Failure(cause) =>
        log.error(s"failed to register market subscription for betting strategy '${bettingStrategy.name}'", cause)
      case QueueOfferResult.QueueClosed =>
        log.error(
          s"unable to register market subscription for betting strategy '${bettingStrategy.name}' because queue is closed"
        )
      case QueueOfferResult.Dropped =>
        log.error(s"dropped market subscription for betting strategy '${bettingStrategy.name}'")
      case QueueOfferResult.Enqueued =>
        log.info(s"enqueued market subscription for betting strategy '${bettingStrategy.name}'")
    }
  }
}

object ActorBettingStrategyOrchestrator {

  def apply(
      betfair: Betfair,
      marketDefinitionsRepository: MarketDefinitionsRepository
  )(implicit system: ActorSystem[_]): BettingStrategyOrchestrator = {
    val actor = system.systemActorOf(
      BettingStrategyOrchestratorBehaviour(marketDefinitionsRepository),
      "betting-strategy-orchestrator"
    )
    new ActorBettingStrategyOrchestrator(betfair, actor)
  }
}

package com.thinkmorestupidless.betfair.streams.marketdefinitions.impl.actor

import com.thinkmorestupidless.betfair.streams.domain.{MarketDefinition, MarketId}
import com.thinkmorestupidless.betfair.streams.marketdefinitions.domain.MarketDefinitionsRepository
import com.thinkmorestupidless.betfair.streams.marketdefinitions.impl.actor.MarketDefinitionsRepositoryBehaviour.{
  Message,
  UpsertMarketDefinition
}
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

final class ActorMarketDefinitionsRepository private (actor: ActorRef[Message])(implicit system: ActorSystem[_])
    extends MarketDefinitionsRepository {

  implicit private val timeout = Timeout(10.seconds)
  implicit private val ec = system.executionContext

  override def updateMarketDefinition(marketId: MarketId, marketDefinition: MarketDefinition): Future[Done] =
    actor.ask[Done](UpsertMarketDefinition(marketId, marketDefinition, _)).map(_ => Done)

  override def getMarketDefinition(marketId: MarketId): Future[Option[MarketDefinition]] = ???
}

object ActorMarketDefinitionsRepository {

  def apply()(implicit system: ActorSystem[_]): MarketDefinitionsRepository = {
    val actor = system.systemActorOf(MarketDefinitionsRepositoryBehaviour(), "market-definitions-repository")
    new ActorMarketDefinitionsRepository(actor)
  }
}

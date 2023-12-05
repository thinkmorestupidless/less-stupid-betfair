package com.thinkmorestupidless.betfair.streams.marketdefinitions.impl.cluster

import com.thinkmorestupidless.betfair.streams.domain.{MarketDefinition, MarketId}
import com.thinkmorestupidless.betfair.streams.marketdefinitions.domain.MarketDefinitionsRepository
import com.thinkmorestupidless.betfair.streams.marketdefinitions.impl.cluster.MarketDefinitionProtocol.{
  Command,
  GetMarketDefinition,
  UpdateMarketDefinition
}
import org.apache.pekko.Done
import org.apache.pekko.actor.Status.{Failure, Status, Success}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import org.apache.pekko.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ShardedDurableStateMarketDefinitionsRepository()(implicit system: ActorSystem[_])
    extends MarketDefinitionsRepository {

  private val sharding = ClusterSharding(system)

  private implicit val ec: ExecutionContext = system.executionContext
  private implicit val timeout: Timeout = Timeout(2.seconds)

  override def updateMarketDefinition(marketId: MarketId, marketDefinition: MarketDefinition): Future[Done] =
    entityRefFor(marketId).ask(replyTo => UpdateMarketDefinition(marketDefinition, replyTo))

  override def getMarketDefinition(marketId: MarketId): Future[Option[MarketDefinition]] =
    entityRefFor(marketId).ask[Status](replyTo => GetMarketDefinition(replyTo)).map {
      case Success(marketDefinition: MarketDefinition) => Some(marketDefinition)
      case Success(_)                                  => None
      case Failure(_)                                  => None
    }

  private def entityRefFor(marketId: MarketId): EntityRef[Command] =
    sharding.entityRefFor(MarketDefinitionShardRegion.TypeKey, marketId.value)
}

object ShardedDurableStateMarketDefinitionsRepository {

  def apply()(implicit system: ActorSystem[_]): MarketDefinitionsRepository = {
    MarketDefinitionShardRegion.init()
    new ShardedDurableStateMarketDefinitionsRepository()
  }
}

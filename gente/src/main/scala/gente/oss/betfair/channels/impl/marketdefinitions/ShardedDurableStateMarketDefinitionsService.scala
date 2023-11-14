package gente.oss.betfair.channels.impl.marketdefinitions

import com.thinkmorestupidless.betfair.streams.domain.{MarketDefinition, MarketId}
import gente.oss.betfair.channels.domain.MarketDefinitionsService
import gente.oss.betfair.channels.impl.marketdefinitions.MarketDefinitionProtocol.{
  Command,
  GetMarketDefinition,
  UpdateMarketDefinition
}
import org.apache.pekko.Done
import org.apache.pekko.actor.Status.{Failure, Status, Success}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import org.apache.pekko.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class ShardedDurableStateMarketDefinitionsService()(implicit system: ActorSystem[_]) extends MarketDefinitionsService {

  private val sharding = ClusterSharding(system)

  private implicit val ec: ExecutionContext = system.executionContext
  private implicit val timeout: Timeout = Timeout(2.seconds)

  override def updateMarketDefinition(marketId: MarketId, marketDefinition: MarketDefinition): Future[Done] =
    entityRefFor(marketId).ask(replyTo => UpdateMarketDefinition(marketDefinition, replyTo))

  override def getMarketDefinition(marketId: MarketId): Future[Option[MarketDefinition]] =
    entityRefFor(marketId).ask[Status](replyTo => GetMarketDefinition(replyTo)).map {
      case Success(marketDefinition: MarketDefinition) => Some(marketDefinition)
      case Success(_)                                  => None
      case Failure(cause)                              => None
    }

  private def entityRefFor(marketId: MarketId): EntityRef[Command] =
    sharding.entityRefFor(MarketDefinitionShardRegion.TypeKey, marketId.value)
}

object ShardedDurableStateMarketDefinitionsService {

  def apply()(implicit system: ActorSystem[_]): MarketDefinitionsService = {
    MarketDefinitionShardRegion.init()
    new ShardedDurableStateMarketDefinitionsService()
  }
}
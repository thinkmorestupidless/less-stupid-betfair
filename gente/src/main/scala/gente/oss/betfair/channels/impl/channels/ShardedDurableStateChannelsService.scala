package gente.oss.betfair.channels.impl.channels

import com.thinkmorestupidless.betfair.streams.domain.MarketFilter
import gente.oss.betfair.channels.domain.{ChannelId, ChannelsService}
import gente.oss.betfair.channels.impl.channels.ChannelProtocol.{Command, GetMarketFilter, UpdateMarketFilter}
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import org.apache.pekko.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

final class ShardedDurableStateChannelsService()(implicit system: ActorSystem[_]) extends ChannelsService {

  private val sharding = ClusterSharding(system)

  private implicit val timeout: Timeout = Timeout(2.seconds)

  override def updateMarketFilterForChannel(channelId: ChannelId, marketFilter: MarketFilter): Future[Done] =
    entityRefFor(channelId).ask(UpdateMarketFilter(marketFilter, _))

  override def getMarketFilterForChannel(channelId: ChannelId): Future[MarketFilter] =
    entityRefFor(channelId).ask(GetMarketFilter(_))

  private def entityRefFor(channelId: ChannelId): EntityRef[Command] =
    sharding.entityRefFor(ChannelShardRegion.TypeKey, channelId.value)
}

object ShardedDurableStateChannelsService {

  def apply()(implicit system: ActorSystem[_]): ChannelsService = {
    ChannelShardRegion.init()
    new ShardedDurableStateChannelsService()
  }
}

package gente.oss.betfair.channels.domain

import com.thinkmorestupidless.betfair.streams.domain.{MarketDefinition, MarketFilter, MarketId}
import org.apache.pekko.Done

import scala.concurrent.Future

final case class ChannelId(value: String)

trait ChannelsService {
  def updateMarketFilterForChannel(channelId: ChannelId, marketFilter: MarketFilter): Future[Done]
  def getMarketFilterForChannel(channelId: ChannelId): Future[MarketFilter]
}

trait MarketDefinitionsService {
  def updateMarketDefinition(marketId: MarketId, marketDefinition: MarketDefinition): Future[Done]
  def getMarketDefinition(marketId: MarketId): Future[Option[MarketDefinition]]
}

sealed trait ToBetfair
sealed trait FromBetfair
sealed trait ToClient
sealed trait FromClient

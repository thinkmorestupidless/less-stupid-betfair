package gente.oss.betfair.channels.domain

import cats.data.EitherT
import com.thinkmorestupidless.betfair.streams.domain.MarketFilter
import org.apache.pekko.Done

import scala.concurrent.Future

final case class ChannelId(value: String)

trait ChannelsService {
  def updateMarketFilterForChannel(channelId: ChannelId, marketFilter: MarketFilter): Future[Done]
  def getMarketFilterForChannel(channelId: ChannelId): Future[MarketFilter]
}

sealed trait ToBetfair
sealed trait FromBetfair
sealed trait ToClient
sealed trait FromClient

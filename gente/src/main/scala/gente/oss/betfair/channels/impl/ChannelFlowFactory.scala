package gente.oss.betfair.channels.impl

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.streams.domain.{IncomingBetfairSocketMessage, OutgoingBetfairSocketMessage}
import gente.oss.betfair.channels.domain.{ChannelId, ChannelsService}
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.Flow

object ChannelFlowFactory {

  type ChannelFlowFactory = ChannelId => Flow[OutgoingBetfairSocketMessage, IncomingBetfairSocketMessage, NotUsed]

  def apply(
      betfair: Betfair,
      channelsService: ChannelsService
  )(implicit
      system: ActorSystem[_]
  ): ChannelFlowFactory = {
    implicit val ec = system.executionContext

    channelId => {
      val channelFilterFlow = ChannelFilterFlow(channelId, channelsService)
      channelFilterFlow.join(betfair.socketFlow.flowFromSinkAndSource())
    }
  }
}

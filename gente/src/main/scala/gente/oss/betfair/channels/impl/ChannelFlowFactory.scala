package gente.oss.betfair.channels.impl

import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.streams.domain.{GlobalMarketFilterRepository, IncomingBetfairSocketMessage, OutgoingBetfairSocketMessage}
import com.thinkmorestupidless.betfair.streams.impl.{BetfairSocketFlow, TlsSocketFlow}
import gente.oss.betfair.channels.domain.{ChannelId, ChannelsService}
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.Flow

object ChannelFlowFactory {

  type ChannelFlowFactory = ChannelId => Flow[OutgoingBetfairSocketMessage, IncomingBetfairSocketMessage, NotUsed]

  def apply(
      socketFlow: TlsSocketFlow.TlsSocketFlow,
      session: BetfairSession,
      globalMarketFilterRepository: GlobalMarketFilterRepository,
      channelsService: ChannelsService
  )(implicit
      system: ActorSystem[_]
  ): ChannelFlowFactory = {
          implicit val ec = system.executionContext
    val betfairSocketFlow = BetfairSocketFlow(socketFlow, session, globalMarketFilterRepository)

    channelId => {
      val channelFilterFlow = ChannelFilterFlow(channelId, channelsService)
      channelFilterFlow.join(betfairSocketFlow.flowFromSinkAndSource())
    }
  }
}

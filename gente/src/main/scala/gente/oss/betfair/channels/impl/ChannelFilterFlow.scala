package gente.oss.betfair.channels.impl

import com.thinkmorestupidless.betfair.streams.domain.{IncomingBetfairSocketMessage, MarketSubscription, OutgoingBetfairSocketMessage}
import gente.oss.betfair.channels.domain.{ChannelId, ChannelsService}
import org.apache.pekko.stream.scaladsl.{BidiFlow, Flow}
import org.apache.pekko.{Done, NotUsed}

import scala.concurrent.{ExecutionContext, Future}

object ChannelFilterFlow {

  type ChannelFilterFlow = BidiFlow[OutgoingBetfairSocketMessage, OutgoingBetfairSocketMessage, IncomingBetfairSocketMessage, IncomingBetfairSocketMessage, NotUsed]

  def apply(channelId: ChannelId, channelsService: ChannelsService)(implicit ec: ExecutionContext): ChannelFilterFlow = {
    val outgoing = Flow[OutgoingBetfairSocketMessage].mapAsync(1) { elem =>
      val future = elem match {
        case MarketSubscription(_, Some(marketFilter)) => channelsService.updateMarketFilterForChannel(channelId, marketFilter)
        case _ => Future.successful(Done)
      }
      future.map(_ => elem)
    }

    val incoming = Flow[IncomingBetfairSocketMessage].map(identity)

    BidiFlow.fromFlows(outgoing, incoming)
  }

}

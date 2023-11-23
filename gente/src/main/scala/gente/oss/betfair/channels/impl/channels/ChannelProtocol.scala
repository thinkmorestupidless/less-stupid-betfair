package gente.oss.betfair.channels.impl.channels

import com.thinkmorestupidless.betfair.streams.domain.MarketFilter
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorRef

object ChannelProtocol {

  sealed trait Command
  final case class UpdateMarketFilter(marketFilter: MarketFilter, replyTo: ActorRef[Done]) extends Command
  final case class GetMarketFilter(replyTo: ActorRef[MarketFilter]) extends Command
}

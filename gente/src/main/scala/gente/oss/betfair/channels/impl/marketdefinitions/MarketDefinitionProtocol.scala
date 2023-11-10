package gente.oss.betfair.channels.impl.marketdefinitions

import com.thinkmorestupidless.betfair.streams.domain.MarketDefinition
import org.apache.pekko.Done
import org.apache.pekko.actor.Status.Status
import org.apache.pekko.actor.typed.ActorRef

object MarketDefinitionProtocol {

  sealed trait Command
  final case class UpdateMarketDefinition(marketDefinition: MarketDefinition, replyTo: ActorRef[Done]) extends Command
  final case class GetMarketDefinition(replyTo: ActorRef[Status]) extends Command

  final case class MarketDefinitionNotInitialized() extends Throwable
}

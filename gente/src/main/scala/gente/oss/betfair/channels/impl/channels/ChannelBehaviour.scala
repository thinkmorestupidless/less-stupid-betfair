package gente.oss.betfair.channels.impl.channels

import com.thinkmorestupidless.betfair.streams.domain.MarketFilter
import gente.oss.betfair.channels.impl.channels.ChannelProtocol.Command
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.state.scaladsl.DurableStateBehavior

object ChannelBehaviour {

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    DurableStateBehavior[Command, MarketFilter](
      persistenceId = persistenceId,
      emptyState = MarketFilter.empty,
      commandHandler = ChannelCommandHandler()
    )
}

package gente.oss.betfair.channels.impl

import gente.oss.betfair.channels.impl.ChannelProtocol.{Command, Event}
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.EventSourcedBehavior

object ChannelBehaviour {

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    EventSourcedBehavior[Command, Event, ChannelState](
      persistenceId = persistenceId,
      emptyState = ChannelState(),
      commandHandler = (state, cmd) => ChannelCommandHandler(),
      eventHandler = (state, evt) => ChannelEventHandler())
}

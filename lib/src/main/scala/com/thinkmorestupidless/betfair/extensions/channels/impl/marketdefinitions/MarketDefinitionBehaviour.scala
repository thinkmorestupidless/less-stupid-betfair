package com.thinkmorestupidless.betfair.extensions.channels.impl.marketdefinitions

import com.thinkmorestupidless.betfair.extensions.channels.impl.marketdefinitions.MarketDefinitionProtocol.Command
import com.thinkmorestupidless.betfair.streams.domain.MarketDefinition
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.state.scaladsl.DurableStateBehavior

object MarketDefinitionBehaviour {

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    DurableStateBehavior[Command, Option[MarketDefinition]](
      persistenceId = persistenceId,
      emptyState = None,
      commandHandler = MarketDefinitionCommandHandler()
    )
}

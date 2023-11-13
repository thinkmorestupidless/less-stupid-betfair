package gente.oss.betfair.channels.impl.tokens

import com.thinkmorestupidless.betfair.vendor.domain.AccessToken
import gente.oss.betfair.channels.impl.tokens.CustomerTokenProtocol.Command
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.state.scaladsl.DurableStateBehavior

object CustomerTokenBehaviour {

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    DurableStateBehavior[Command, Option[AccessToken]](
      persistenceId = persistenceId,
      emptyState = None,
      commandHandler = CustomerTokenCommandHandler()
    )
}

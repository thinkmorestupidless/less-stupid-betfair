package gente.oss.betfair.channels.impl

import gente.oss.betfair.channels.impl.ChannelProtocol.Command
import org.apache.pekko.persistence.typed.state.scaladsl.Effect

object ChannelCommandHandler {

  def apply(): (ChannelState, Command) => Effect[ChannelState] = {
    ???
  }
}

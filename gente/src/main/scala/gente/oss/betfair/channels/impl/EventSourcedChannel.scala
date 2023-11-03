package gente.oss.betfair.channels.impl

import gente.oss.betfair.channels.domain.Channel

class EventSourcedChannel extends Channel {

}

object EventSourcedChannel {

  def apply(): EventSourcedChannel = {
    new EventSourcedChannel()
  }
}

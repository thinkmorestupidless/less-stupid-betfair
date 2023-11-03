package gente.oss.betfair.channels.impl

object ChannelProtocol {

  sealed trait Command

  sealed trait Event

  sealed trait Response
}

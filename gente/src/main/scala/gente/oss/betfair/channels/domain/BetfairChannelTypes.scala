package gente.oss.betfair.channels.domain

final case class ChannelId(value: String)

trait Channel

sealed trait ToBetfair
sealed trait FromBetfair
sealed trait ToClient
sealed trait FromClient

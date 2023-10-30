package com.thinkmorestupidless.betfair.streams.impl

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Flow
import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.streams.domain.{
  GlobalMarketFilterRepository,
  IncomingBetfairSocketMessage,
  OutgoingBetfairSocketMessage
}

object BetfairSocketFlow {

  type BetfairSocketFlow =
    Flow[OutgoingBetfairSocketMessage, IncomingBetfairSocketMessage, NotUsed]

  def apply(
      socketFlow: TlsSocketFlow.TlsSocketFlow,
      session: BetfairSession,
      globalMarketFilterRepository: GlobalMarketFilterRepository
  )(implicit system: ActorSystem[_]): BetfairSocketFlow = {
    val codecFlow = BetfairCodecFlow().join(socketFlow)
    BetfairProtocolFlow(session, globalMarketFilterRepository).join(codecFlow)
  }
}

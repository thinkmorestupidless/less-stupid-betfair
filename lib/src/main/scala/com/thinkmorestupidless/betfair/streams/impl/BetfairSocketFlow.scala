package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.streams.domain.{GlobalMarketFilterRepository, IncomingBetfairSocketMessage, OutgoingBetfairSocketMessage}
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, RunnableGraph, Sink, Source}

final class BetfairSocketFlow(hub: RunnableGraph[(Sink[OutgoingBetfairSocketMessage, NotUsed], Source[IncomingBetfairSocketMessage, NotUsed])])(implicit system: ActorSystem[_]) {

  def sinkAndSource(): (Sink[OutgoingBetfairSocketMessage, NotUsed], Source[IncomingBetfairSocketMessage, NotUsed]) =
    hub.run()

  def flowFromSinkAndSource(): Flow[OutgoingBetfairSocketMessage, IncomingBetfairSocketMessage, NotUsed] = {
    val (sink, source) = sinkAndSource()
    Flow.fromSinkAndSource(sink, source)
  }
}

object BetfairSocketFlow {

  def apply(
      socketFlow: TlsSocketFlow.TlsSocketFlow,
      session: BetfairSession,
      globalMarketFilterRepository: GlobalMarketFilterRepository
  )(implicit system: ActorSystem[_]): BetfairSocketFlow = {
    val codecFlow = BetfairCodecFlow().join(socketFlow)
    val betfairSocketFlow = BetfairProtocolFlow(session, globalMarketFilterRepository).join(codecFlow)
    val x: RunnableGraph[(Sink[OutgoingBetfairSocketMessage, NotUsed], Source[IncomingBetfairSocketMessage, NotUsed])] = MergeHub
      .source[OutgoingBetfairSocketMessage](perProducerBufferSize = 16)
      .via(betfairSocketFlow)
      .toMat(BroadcastHub.sink[IncomingBetfairSocketMessage](bufferSize = 256))(Keep.both)
    new BetfairSocketFlow(x)
  }
}

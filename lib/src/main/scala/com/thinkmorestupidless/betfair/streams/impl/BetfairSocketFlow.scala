package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, SessionToken}
import com.thinkmorestupidless.betfair.streams.domain.{
  GlobalMarketFilterRepository,
  Heartbeat,
  IncomingBetfairSocketMessage,
  OutgoingBetfairSocketMessage
}
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}

final class BetfairSocketFlow(
    val sink: Sink[OutgoingBetfairSocketMessage, NotUsed],
    val source: Source[IncomingBetfairSocketMessage, NotUsed]
) {

  def flowFromSinkAndSource(): Flow[OutgoingBetfairSocketMessage, IncomingBetfairSocketMessage, NotUsed] =
    Flow.fromSinkAndSource(sink, source)
}

object BetfairSocketFlow {

  def apply(
      socketFlow: TlsSocketFlow.TlsSocketFlow,
      applicationKey: ApplicationKey,
      sessionToken: SessionToken,
      globalMarketFilterRepository: GlobalMarketFilterRepository
  )(implicit system: ActorSystem[_]): BetfairSocketFlow = {
    val codecFlow = BetfairCodecFlow().join(socketFlow)
    val betfairSocketFlow =
      BetfairProtocolFlow(applicationKey, sessionToken, globalMarketFilterRepository).join(codecFlow)
    val (sink, source) =
      MergeHub
        .source[OutgoingBetfairSocketMessage](perProducerBufferSize = 16)
        .via(betfairSocketFlow)
        .toMat(BroadcastHub.sink[IncomingBetfairSocketMessage](bufferSize = 256))(Keep.both)
        .run()

    // We send a heartbeat here to kickstart the stream
    // Without it there's no demand until a user request arrives
    // The TCP socket won't connect until there's demand in the stream.
    Source.single(Heartbeat()).runWith(sink)

    new BetfairSocketFlow(sink, source)
  }
}

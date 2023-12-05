package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, SessionToken}
import com.thinkmorestupidless.betfair.core.impl.OutgoingHeartbeat
import com.thinkmorestupidless.betfair.streams.domain.{
  GlobalMarketFilterRepository,
  Heartbeat,
  IncomingBetfairSocketMessage,
  MarketSubscription,
  OutgoingBetfairSocketMessage
}
import com.thinkmorestupidless.betfair.streams.marketdefinitions.domain.MarketDefinitionsRepository
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.Framing.FramingException
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.{ActorAttributes, Supervision}
import org.slf4j.LoggerFactory

final class BetfairSocket(
    val sink: Sink[OutgoingBetfairSocketMessage, NotUsed],
    val source: Source[IncomingBetfairSocketMessage, NotUsed]
)(implicit system: ActorSystem[_]) {

  val marketSubscriptionQueue = {
    val (queue, source) = Source.queue[MarketSubscription](100).preMaterialize()
    source.runWith(sink)
    queue
  }

  def flowFromSinkAndSource(): Flow[OutgoingBetfairSocketMessage, IncomingBetfairSocketMessage, NotUsed] =
    Flow.fromSinkAndSource(sink, source)
}

object BetfairSocket {

  private val log = LoggerFactory.getLogger(getClass)

  private val handleStreamFailures: Supervision.Decider = {
    case e: FramingException =>
      log.error(
        s"stream failed with FramingException '${e.getMessage}' - this is usually a sign you need to increase the frame size of the socket. Check config value betfair.exchange.socket.frame-size"
      )
      Supervision.Stop
    case _ => Supervision.Stop
  }

  def apply(
      socketFlow: TlsSocketFlow.TlsSocketFlow,
      applicationKey: ApplicationKey,
      sessionToken: SessionToken,
      globalMarketFilterRepository: GlobalMarketFilterRepository,
      marketDefinitionsRepository: MarketDefinitionsRepository,
      outgoingHeartbeat: OutgoingHeartbeat
  )(implicit system: ActorSystem[_]): BetfairSocket = {
    val codecFlow = BetfairCodeBidiFlow().join(socketFlow)
    val betfairSocketFlow =
      BetfairSocketBidiFlow(
        applicationKey,
        sessionToken,
        globalMarketFilterRepository,
        marketDefinitionsRepository,
        outgoingHeartbeat
      ).join(codecFlow).withAttributes(ActorAttributes.supervisionStrategy(handleStreamFailures))

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

    new BetfairSocket(sink, source)
  }
}

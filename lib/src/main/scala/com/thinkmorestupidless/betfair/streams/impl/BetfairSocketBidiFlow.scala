package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, SessionToken}
import com.thinkmorestupidless.betfair.core.impl.OutgoingHeartbeat
import com.thinkmorestupidless.betfair.streams.domain.{
  GlobalMarketFilterRepository,
  IncomingBetfairSocketMessage,
  OutgoingBetfairSocketMessage
}
import com.thinkmorestupidless.betfair.streams.marketdefinitions.domain.MarketDefinitionsRepository
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.BidiShape
import org.apache.pekko.stream.scaladsl.{BidiFlow, Broadcast, GraphDSL, Merge, Source}

object BetfairSocketBidiFlow {

  type BetfairSocketBidiFlow = BidiFlow[
    OutgoingBetfairSocketMessage,
    OutgoingBetfairSocketMessage,
    IncomingBetfairSocketMessage,
    IncomingBetfairSocketMessage,
    NotUsed
  ]

  def apply(
      applicationKey: ApplicationKey,
      sessionToken: SessionToken,
      globalMarketFilterRepository: GlobalMarketFilterRepository,
      marketDefinitionsRepository: MarketDefinitionsRepository,
      outgoingHeartbeat: OutgoingHeartbeat
  )(implicit
      system: ActorSystem[_]
  ): BetfairSocketBidiFlow =
    BidiFlow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      implicit val ec = system.executionContext

      val (queue, source) = Source.queue[OutgoingBetfairSocketMessage](bufferSize = 100).preMaterialize()

      val protocolFlows = BetfairProtocolFlows(applicationKey, sessionToken)

      val incomingProtocolFlow = b.add(protocolFlows.incoming)
      val outgoingProtocolFlow = b.add(protocolFlows.outgoing)

      val mergeIncoming = b.add(Merge[IncomingBetfairSocketMessage](inputPorts = 2))
      val mergeOutgoing = b.add(Merge[OutgoingBetfairSocketMessage](inputPorts = 3))
      val splitIncoming = b.add(BetfairMessageSplitter())
      val splitOutgoing = b.add(BetfairMessageSplitter())
      val broadcastOutgoing = b.add(Broadcast[OutgoingBetfairSocketMessage](outputPorts = 2))
      val heartbeatFlow = b.add(BetfairHeartbeatFlow(queue, source, outgoingHeartbeat))
      val upsertMarketDefinition = b.add(UpdateMarketDefinitionsFlow(marketDefinitionsRepository))
      val updateGlobalMarketFilter = b.add(UpdateGlobalMarketFilterFlow(globalMarketFilterRepository))

      incomingProtocolFlow.out ~> splitIncoming.in

      splitIncoming.outgoing ~> mergeOutgoing.in(0)
      splitIncoming.incoming ~> upsertMarketDefinition.in
      upsertMarketDefinition.out ~> mergeIncoming.in(0)

      outgoingProtocolFlow.out ~> splitOutgoing.in

      splitOutgoing.outgoing ~> broadcastOutgoing.in
      splitOutgoing.incoming ~> mergeIncoming.in(1)

      broadcastOutgoing.out(0) ~> mergeOutgoing.in(1)
      broadcastOutgoing.out(1) ~> heartbeatFlow.in

      heartbeatFlow.out ~> mergeOutgoing.in(2)
      mergeOutgoing.out ~> updateGlobalMarketFilter.in

      BidiShape(outgoingProtocolFlow.in, updateGlobalMarketFilter.out, incomingProtocolFlow.in, mergeIncoming.out)
    })
}

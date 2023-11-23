package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, SessionToken}
import com.thinkmorestupidless.betfair.core.domain.{SocketAuthenticated, SocketAuthenticationFailed, SocketConnected}
import com.thinkmorestupidless.betfair.core.impl.OutgoingHeartbeat
import com.thinkmorestupidless.betfair.streams.domain._
import com.thinkmorestupidless.betfair.streams.impl.BetfairProtocolActor.{
  Answer,
  BetfairProtocolMessage,
  IncomingQuestion,
  OutgoingQuestion
}
import com.thinkmorestupidless.extensions.akkastreams.SplitEither
import com.thinkmorestupidless.utils.RandomUtils
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.eventstream.EventStream.Publish
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import org.apache.pekko.stream.BidiShape
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.typed.scaladsl.ActorFlow
import org.apache.pekko.util.Timeout

import scala.concurrent.duration._

object BetfairProtocolFlow {

  type BetfairProtocolFlow = BidiFlow[
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
      outgoingHeartbeat: OutgoingHeartbeat
  )(implicit
      system: ActorSystem[_]
  ): BetfairProtocolFlow =
    BidiFlow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      implicit val timeout = Timeout(10.seconds)

      val (queue, source) = Source.queue[OutgoingBetfairSocketMessage](bufferSize = 100).preMaterialize()

      system.systemActorOf(
        GlobalMarketSubscriptionSupplierActor(globalMarketFilterRepository, queue),
        "betfair-global-filter"
      )

      val protocolActor: ActorRef[BetfairProtocolMessage] = system.systemActorOf(
        BetfairProtocolActor(applicationKey, sessionToken),
        name = s"betfair-socket-protocol-${RandomUtils.generateRandomString()}",
        Props.empty
      )

      val mergeIncoming = b.add(Merge[IncomingBetfairSocketMessage](inputPorts = 2))
      val mergeOutgoing = b.add(Merge[OutgoingBetfairSocketMessage](inputPorts = 3))
      val splitIncoming = b.add(SplitEither[IncomingBetfairSocketMessage, OutgoingBetfairSocketMessage])
      val splitOutgoing = b.add(SplitEither[IncomingBetfairSocketMessage, OutgoingBetfairSocketMessage])
      val broadcastOutgoing = b.add(Broadcast[OutgoingBetfairSocketMessage](outputPorts = 2))
      val heartbeatFlow = b.add(BetfairHeartbeatFlow(queue, source, outgoingHeartbeat))

      val handleSplitResult =
        Flow[Answer].mapConcat(_.messages).map {
          case incoming: IncomingBetfairSocketMessage => Left(incoming)
          case outgoing: OutgoingBetfairSocketMessage => Right(outgoing)
        }

      val incomingProtocolFlow = b.add(
        ActorFlow
          .ask[IncomingBetfairSocketMessage, IncomingQuestion, Answer](protocolActor)(IncomingQuestion(_, _))
          .via(handleSplitResult)
      )

      val outgoingProtocolFlow = b.add(
        ActorFlow
          .ask[OutgoingBetfairSocketMessage, OutgoingQuestion, Answer](protocolActor)(OutgoingQuestion(_, _))
          .via(handleSplitResult)
      )

      incomingProtocolFlow.out ~> splitIncoming.in

      splitIncoming.right ~> mergeOutgoing.in(0)
      splitIncoming.left ~> mergeIncoming.in(0)

      outgoingProtocolFlow.out ~> splitOutgoing.in

      splitOutgoing.right ~> broadcastOutgoing.in
      splitOutgoing.left ~> mergeIncoming.in(1)

      broadcastOutgoing.out(0) ~> mergeOutgoing.in(1)
      broadcastOutgoing.out(1) ~> heartbeatFlow.in

      heartbeatFlow.out ~> mergeOutgoing.in(2)

      BidiShape(outgoingProtocolFlow.in, mergeOutgoing.out, incomingProtocolFlow.in, mergeIncoming.out)
    })
}

private object BetfairProtocolActor {

  sealed trait BetfairProtocolMessage
  final case class IncomingQuestion(incoming: IncomingBetfairSocketMessage, replyTo: ActorRef[Answer])
      extends BetfairProtocolMessage
  final case class OutgoingQuestion(outgoing: OutgoingBetfairSocketMessage, replyTo: ActorRef[Answer])
      extends BetfairProtocolMessage

  final case class Answer(messages: List[BetfairSocketMessage])

  def apply(
      applicationKey: ApplicationKey,
      sessionToken: SessionToken
  ): Behavior[BetfairProtocolMessage] =
    Behaviors.withStash(capacity = 100) { buffer =>
      Behaviors.setup { context =>
        def unconnected(
            sessionToken: SessionToken
        ): Behavior[BetfairProtocolMessage] =
          Behaviors.receiveMessagePartial[BetfairProtocolMessage] {
            case IncomingQuestion(Connection(_, _), replyTo) =>
              context.system.log.info(s"[unconnected] received connection, giving back authentication message")
              context.system.eventStream ! Publish(SocketConnected)
              replyTo ! Answer(List(Authentication(applicationKey, sessionToken)))
              connecting()

            case message =>
              context.system.log.info(s"received message '$message'")
              buffer.stash(message)
              Behaviors.same
          }

        def connecting(): Behavior[BetfairProtocolMessage] =
          Behaviors.receiveMessagePartial[BetfairProtocolMessage] {
            case IncomingQuestion(Success(_), replyTo) =>
              context.system.log.info(s"${context.self.path.name} authentication success, moving to connected")
              context.system.eventStream ! Publish(SocketAuthenticated)
              replyTo ! Answer(List.empty)
              buffer.unstashAll(connected())

            case IncomingQuestion(Failure(_, _, _, _), replyTo) =>
              context.system.log.info(s"${context.self.path.name} authentication failure, stopping")
              context.system.eventStream ! Publish(SocketAuthenticationFailed)
              replyTo ! Answer(List(SocketFailed))
              Behaviors.same

            case message =>
              buffer.stash(message)
              Behaviors.same
          }

        def connected(): Behavior[BetfairProtocolMessage] = {
          def handle(message: IncomingBetfairSocketMessage): List[IncomingBetfairSocketMessage] =
            message match {
              case mcm: MarketChangeMessage => List(mcm)
              case _                        => List.empty
            }

          Behaviors.receiveMessagePartial[BetfairProtocolMessage] {
            case IncomingQuestion(message, replyTo) =>
              replyTo ! Answer(handle(message))
              Behaviors.same

            case OutgoingQuestion(message, replyTo) =>
              replyTo ! Answer(List(message))
              Behaviors.same
          }
        }

        unconnected(sessionToken)
      }
    }
}

package com.thinkmorestupidless.betfair.streams.impl

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import akka.stream.BidiShape
import akka.stream.scaladsl.{BidiFlow, GraphDSL, Merge}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.streams.domain._
import com.thinkmorestupidless.betfair.streams.impl.BetfairProtocolActor.{Answer, Question}
import com.thinkmorestupidless.extensions.akkastreams.SplitEither
import com.thinkmorestupidless.utils.RandomUtils
import io.circe

import scala.concurrent.duration._

object BetfairProtocolFlow {

  type BetfairProtocolFlow = BidiFlow[
    OutgoingBetfairSocketMessage,
    OutgoingBetfairSocketMessage,
    Either[circe.Error, IncomingBetfairSocketMessage],
    Either[circe.Error, IncomingBetfairSocketMessage],
    NotUsed
  ]

  def apply(session: BetfairSession, globalMarketFilterRepository: GlobalMarketFilterRepository)(implicit system: ActorSystem[_]): BetfairProtocolFlow = {
    implicit val timeout = Timeout(10.seconds)

    val protocolActor: ActorRef[Question] = system.systemActorOf(
      BetfairProtocolActor(session, globalMarketFilterRepository),
      name = s"betfair-socket-protocol-${RandomUtils.generateRandomString()}",
      Props.empty
    )

    BidiFlow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val mergeOutgoing = b.add(Merge[OutgoingBetfairSocketMessage](inputPorts = 2))
      val splitIncoming = b.add(SplitEither[IncomingBetfairSocketMessage, OutgoingBetfairSocketMessage])

      val protocolFlow = b.add(
        ActorFlow
          .ask[IncomingBetfairSocketMessage, Question, Answer](protocolActor)(makeMessage =
            (el, replyTo) => Question(el, replyTo)
          )
          .mapConcat(_.messages)
          .map {
            case incoming: IncomingBetfairSocketMessage => Left(incoming)
            case outgoing: OutgoingBetfairSocketMessage => Right(outgoing)
          }
      )

      protocolFlow.out ~> splitIncoming.in
      splitIncoming.right ~> mergeOutgoing.in(1)

      BidiShape(mergeOutgoing.in(0), mergeOutgoing.out, protocolFlow.in, splitIncoming.left)
    })
  }
}

private object BetfairProtocolActor {

  sealed trait BetfairProtocolMessage
  final case class SetGlobalMarketFilter(marketFilter: MarketFilter) extends BetfairProtocolMessage
  final case class InitializationError(cause: Throwable) extends BetfairProtocolMessage
  final case class Question(incoming: IncomingBetfairSocketMessage, replyTo: ActorRef[Answer]) extends BetfairProtocolMessage

  final case class Answer(messages: List[BetfairSocketMessage])

  def apply(session: BetfairSession, globalMarketFilterRepository: GlobalMarketFilterRepository): Behavior[BetfairProtocolMessage] = {
    Behaviors.withStash(capacity = 100) { buffer =>
      Behaviors.setup { context =>
        implicit val timeout = Timeout(3.seconds)

        def initializing(): Behavior[BetfairProtocolMessage] =
          Behaviors.receiveMessagePartial[BetfairProtocolMessage] {
            case SetGlobalMarketFilter(marketFilter) =>
              context.system.log.info(s"${context.self.path.name} received market filter - moving to unconnected")
              unconnected(marketFilter)

            case InitializationError(cause) =>
              throw cause

            case message =>
              buffer.stash(message)
              Behaviors.same
          }

        def unconnected(marketFilter: MarketFilter): Behavior[BetfairProtocolMessage] =
          Behaviors.receiveMessagePartial[BetfairProtocolMessage] {
            case Question(Connection(_, _), replyTo) =>
              context.system.log
                .info(s"${context.self.path.name} received connection, giving back authentication message")
              replyTo ! Answer(List(Authentication(session.applicationKey, session.sessionToken)))
              connecting(marketFilter)

            case asking: Question =>
              buffer.stash(asking)
              Behaviors.same
          }

        def connecting(marketFilter: MarketFilter): Behavior[BetfairProtocolMessage] =
          Behaviors.receiveMessagePartial[BetfairProtocolMessage] {
            case Question(Success(_), replyTo) =>
              context.system.log.info(s"${context.self.path.name} authentication success, moving to connected")
              val outgoing =
                if (marketFilter == MarketFilter.empty) Nil else List(MarketSubscription(marketFilter))
              replyTo ! Answer(List(SocketReady) ++ outgoing)
              buffer.unstashAll(connected())

            case Question(Failure(_, _, _, _), replyTo) =>
              context.system.log.info(s"${context.self.path.name} authentication failure, stopping")
              replyTo ! Answer(List(SocketFailed))
              Behaviors.same

            case asking: Question =>
              buffer.stash(asking)
              Behaviors.same
          }

        def connected(): Behavior[BetfairProtocolMessage] = {
          def handle(message: IncomingBetfairSocketMessage): List[IncomingBetfairSocketMessage] =
            message match {
              case mcm: MarketChangeMessage => List(mcm)
              case _ => List.empty
            }

          Behaviors.receiveMessagePartial[BetfairProtocolMessage] { case Question(message, replyTo) =>
            replyTo ! Answer(handle(message))
            Behaviors.same
          }
        }

        context.pipeToSelf(globalMarketFilterRepository.getCurrentGlobalFilter()) {
          case scala.util.Success(globalMarketFilter) => SetGlobalMarketFilter(globalMarketFilter)
          case scala.util.Failure(cause) => InitializationError(cause)
        }

        initializing()
      }
    }
  }
}

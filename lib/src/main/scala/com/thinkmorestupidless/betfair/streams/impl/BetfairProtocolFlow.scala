package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.AuthenticationError
import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, SessionToken}
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
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import org.apache.pekko.stream.BidiShape
import org.apache.pekko.stream.scaladsl.{BidiFlow, Broadcast, GraphDSL, Merge}
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
      globalMarketFilter: MarketFilter
  )(implicit
      system: ActorSystem[_]
  ): BetfairProtocolFlow = {
    implicit val timeout = Timeout(10.seconds)

    val protocolActor: ActorRef[BetfairProtocolMessage] = system.systemActorOf(
      BetfairProtocolActor(applicationKey, sessionToken, globalMarketFilter),
      name = s"betfair-socket-protocol-${RandomUtils.generateRandomString()}",
      Props.empty
    )

    BidiFlow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val mergeIncoming = b.add(Merge[IncomingBetfairSocketMessage](inputPorts = 2))
      val mergeOutgoing = b.add(Merge[OutgoingBetfairSocketMessage](inputPorts = 3))
      val splitIncoming = b.add(SplitEither[IncomingBetfairSocketMessage, OutgoingBetfairSocketMessage])
      val splitOutgoing = b.add(SplitEither[IncomingBetfairSocketMessage, OutgoingBetfairSocketMessage])
      val broadcastOutgoing = b.add(Broadcast[OutgoingBetfairSocketMessage](outputPorts = 2))
      val heartbeatFlow = b.add(BetfairHeartbeatFlow())

      val incomingProtocolFlow = b.add(
        ActorFlow
          .ask[IncomingBetfairSocketMessage, IncomingQuestion, Answer](protocolActor)(makeMessage =
            (el, replyTo) => IncomingQuestion(el, replyTo)
          )
          .mapConcat(_.messages)
          .map {
            case incoming: IncomingBetfairSocketMessage => Left(incoming)
            case outgoing: OutgoingBetfairSocketMessage => Right(outgoing)
          }
      )

      val outgoingProtocolFlow = b.add(
        ActorFlow
          .ask[OutgoingBetfairSocketMessage, OutgoingQuestion, Answer](protocolActor)(makeMessage =
            (el, replyTo) => OutgoingQuestion(el, replyTo)
          )
          .mapConcat(_.messages)
          .map {
            case incoming: IncomingBetfairSocketMessage => Left(incoming)
            case outgoing: OutgoingBetfairSocketMessage => Right(outgoing)
          }
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
}

private object BetfairProtocolActor {

  sealed trait BetfairProtocolMessage
  final case class SetGlobalMarketFilter(marketFilter: MarketFilter) extends BetfairProtocolMessage
  final case class InitializationError(cause: Throwable) extends BetfairProtocolMessage
  final case class SetSessionToken(sessionToken: SessionToken) extends BetfairProtocolMessage
  final case class ErrorGettingSessionToken(error: AuthenticationError) extends BetfairProtocolMessage
  final case class FailedToGetSessionToken(cause: Throwable) extends BetfairProtocolMessage
  final case class IncomingQuestion(incoming: IncomingBetfairSocketMessage, replyTo: ActorRef[Answer])
      extends BetfairProtocolMessage
  final case class OutgoingQuestion(outgoing: OutgoingBetfairSocketMessage, replyTo: ActorRef[Answer])
      extends BetfairProtocolMessage

  final case class Answer(messages: List[BetfairSocketMessage])

  def apply(
      applicationKey: ApplicationKey,
      sessionToken: SessionToken,
      globalMarketFilter: MarketFilter
  ): Behavior[BetfairProtocolMessage] =
    Behaviors.withStash(capacity = 100) { buffer =>
      Behaviors.setup { context =>
        implicit val timeout = Timeout(3.seconds)

//        def waitingForSessionToken(): Behavior[BetfairProtocolMessage] =
//          Behaviors.receiveMessage[BetfairProtocolMessage] {
//            case SetSessionToken(sessionToken) =>
//              context.system.log.info("session token received, getting global market filter")
//              context.pipeToSelf(globalMarketFilterRepository.getCurrentGlobalFilter()) {
//                case scala.util.Success(globalMarketFilter) => SetGlobalMarketFilter(globalMarketFilter)
//                case scala.util.Failure(cause)              => InitializationError(cause)
//              }
//              waitingForGlobalMarketFilter(sessionToken)
//
//            case ErrorGettingSessionToken(error) =>
//              throw new RuntimeException(s"Failed to get session token '$error'")
//
//            case FailedToGetSessionToken(cause) =>
//              throw cause
//
//            case message =>
//              context.system.log.info(s"[waitingForSessionToken] received message '$message'")
//              buffer.stash(message)
//              Behaviors.same
//          }

//        def waitingForGlobalMarketFilter(sessionToken: SessionToken): Behavior[BetfairProtocolMessage] =
//          Behaviors.receiveMessage[BetfairProtocolMessage] {
//            case SetGlobalMarketFilter(marketFilter) =>
//              context.system.log
//                .info(s"[waitingForGlobalMarketFilter] received global market filter - moving to unconnected")
//              unconnected(sessionToken, marketFilter)
//
//            case InitializationError(cause) =>
//              throw cause
//
//            case message =>
//              context.system.log.info(s"received message '$message'")
//              buffer.stash(message)
//              Behaviors.same
//          }

        def unconnected(
            sessionToken: SessionToken,
            maybeMarketSubscription: Option[MarketSubscription]
        ): Behavior[BetfairProtocolMessage] =
          Behaviors.receiveMessagePartial[BetfairProtocolMessage] {
            case IncomingQuestion(Connection(_, _), replyTo) =>
              context.system.log.info(s"[unconnected] received connection, giving back authentication message")
              replyTo ! Answer(List(Authentication(applicationKey, sessionToken)))
              connecting(maybeMarketSubscription)

            case OutgoingQuestion(marketSubscription: MarketSubscription, replyTo) =>
              replyTo ! Answer(List.empty)
              unconnected(sessionToken, Some(marketSubscription))

            case message =>
              context.system.log.info(s"received message '$message'")
              buffer.stash(message)
              Behaviors.same
          }

        def connecting(maybeMarketSubscription: Option[MarketSubscription]): Behavior[BetfairProtocolMessage] =
          Behaviors.receiveMessagePartial[BetfairProtocolMessage] {
            case IncomingQuestion(Success(_), replyTo) =>
              context.system.log.info(s"${context.self.path.name} authentication success, moving to connected")
              replyTo ! Answer(maybeMarketSubscription.toList)
              buffer.unstashAll(connected())

            case IncomingQuestion(Failure(_, _, _, _), replyTo) =>
              context.system.log.info(s"${context.self.path.name} authentication failure, stopping")
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

//        context.pipeToSelf(authenticationService.login().value) {
//          case scala.util.Success(sessionTokenOr) =>
//            sessionTokenOr match {
//              case Right(sessionToken) => SetSessionToken(sessionToken)
//              case Left(error)         => ErrorGettingSessionToken(error)
//            }
//          case scala.util.Failure(cause) => FailedToGetSessionToken(cause)
//        }
//
//        waitingForSessionToken()

//        context.pipeToSelf(globalMarketFilterRepository.getCurrentGlobalFilter()) {
//          case scala.util.Success(globalMarketFilter) => SetGlobalMarketFilter(globalMarketFilter)
//          case scala.util.Failure(cause) => InitializationError(cause)
//        }
//
//        waitingForGlobalMarketFilter(sessionToken)

        unconnected(sessionToken, None)
      }
    }
}

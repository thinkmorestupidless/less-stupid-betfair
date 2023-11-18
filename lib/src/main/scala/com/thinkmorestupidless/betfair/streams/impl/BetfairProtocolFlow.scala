package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.AuthenticationError
import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, BetfairAuthenticationService, SessionToken}
import com.thinkmorestupidless.betfair.streams.domain._
import com.thinkmorestupidless.betfair.streams.impl.BetfairProtocolActor.{Answer, Question}
import com.thinkmorestupidless.extensions.akkastreams.SplitEither
import com.thinkmorestupidless.utils.RandomUtils
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import org.apache.pekko.stream.BidiShape
import org.apache.pekko.stream.scaladsl.{BidiFlow, GraphDSL, Merge}
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
      authenticationService: BetfairAuthenticationService,
      globalMarketFilterRepository: GlobalMarketFilterRepository
  )(implicit
      system: ActorSystem[_]
  ): BetfairProtocolFlow = {
    implicit val timeout = Timeout(10.seconds)

    val protocolActor: ActorRef[Question] = system.systemActorOf(
      BetfairProtocolActor(applicationKey, authenticationService, globalMarketFilterRepository),
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
  final case class SetSessionToken(sessionToken: SessionToken) extends BetfairProtocolMessage
  final case class ErrorGettingSessionToken(error: AuthenticationError) extends BetfairProtocolMessage
  final case class FailedToGetSessionToken(cause: Throwable) extends BetfairProtocolMessage
  final case class Question(incoming: IncomingBetfairSocketMessage, replyTo: ActorRef[Answer])
      extends BetfairProtocolMessage

  final case class Answer(messages: List[BetfairSocketMessage])

  def apply(
      applicationKey: ApplicationKey,
      authenticationService: BetfairAuthenticationService,
      globalMarketFilterRepository: GlobalMarketFilterRepository
  ): Behavior[BetfairProtocolMessage] =
    Behaviors.withStash(capacity = 100) { buffer =>
      Behaviors.setup { context =>
        implicit val timeout = Timeout(3.seconds)

        def waitingForSessionToken(): Behavior[BetfairProtocolMessage] =
          Behaviors.receiveMessage[BetfairProtocolMessage] {
            case SetSessionToken(sessionToken) =>
              context.system.log.info("session token received, getting global market filter")
              context.pipeToSelf(globalMarketFilterRepository.getCurrentGlobalFilter()) {
                case scala.util.Success(globalMarketFilter) => SetGlobalMarketFilter(globalMarketFilter)
                case scala.util.Failure(cause)              => InitializationError(cause)
              }
              waitingForGlobalMarketFilter(sessionToken)

            case ErrorGettingSessionToken(error) =>
              throw new RuntimeException(s"Failed to get session token '$error'")

            case FailedToGetSessionToken(cause) =>
              throw cause

            case message =>
              buffer.stash(message)
              Behaviors.same
          }

        def waitingForGlobalMarketFilter(sessionToken: SessionToken): Behavior[BetfairProtocolMessage] =
          Behaviors.receiveMessage[BetfairProtocolMessage] {
            case SetGlobalMarketFilter(marketFilter) =>
              context.system.log
                .info(s"${context.self.path.name} received global market filter - moving to unconnected")
              unconnected(sessionToken, marketFilter)

            case InitializationError(cause) =>
              throw cause

            case message =>
              buffer.stash(message)
              Behaviors.same
          }

        def unconnected(sessionToken: SessionToken, marketFilter: MarketFilter): Behavior[BetfairProtocolMessage] =
          Behaviors.receiveMessagePartial[BetfairProtocolMessage] {
            case Question(Connection(_, _), replyTo) =>
              context.system.log
                .info(s"${context.self.path.name} received connection, giving back authentication message")
              replyTo ! Answer(List(Authentication(applicationKey, sessionToken)))
              connecting(marketFilter)

            case question: Question =>
              buffer.stash(question)
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
              case _                        => List.empty
            }

          Behaviors.receiveMessagePartial[BetfairProtocolMessage] { case Question(message, replyTo) =>
            replyTo ! Answer(handle(message))
            Behaviors.same
          }
        }

        context.pipeToSelf(authenticationService.login().value) {
          case scala.util.Success(sessionTokenOr) =>
            sessionTokenOr match {
              case Right(sessionToken) => SetSessionToken(sessionToken)
              case Left(error)         => ErrorGettingSessionToken(error)
            }
          case scala.util.Failure(cause) => FailedToGetSessionToken(cause)
        }

        waitingForSessionToken()
      }
    }
}

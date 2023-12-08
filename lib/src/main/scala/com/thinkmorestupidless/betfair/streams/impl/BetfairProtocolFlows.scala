package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, SessionToken}
import com.thinkmorestupidless.betfair.core.domain.{
  MarketFilterUpdate,
  SocketAuthenticated,
  SocketAuthenticationFailed,
  SocketConnected
}
import com.thinkmorestupidless.betfair.streams.domain._
import com.thinkmorestupidless.betfair.streams.impl.BetfairProtocolActor.{
  Answer,
  BetfairProtocolMessage,
  IncomingQuestion,
  OutgoingQuestion
}
import com.thinkmorestupidless.betfair.streams.impl.BetfairProtocolFlows.{IncomingFlow, OutgoingFlow}
import com.thinkmorestupidless.utils.RandomUtils
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.eventstream.EventStream.Publish
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.typed.scaladsl.ActorFlow
import org.apache.pekko.util.Timeout

import scala.concurrent.duration._

final case class BetfairProtocolFlows(incoming: IncomingFlow, outgoing: OutgoingFlow)

object BetfairProtocolFlows {

  type IncomingFlow = Flow[IncomingBetfairSocketMessage, BetfairSocketMessage, NotUsed]
  type OutgoingFlow = Flow[OutgoingBetfairSocketMessage, BetfairSocketMessage, NotUsed]

  def apply(applicationKey: ApplicationKey, sessionToken: SessionToken)(implicit
      system: ActorSystem[_]
  ): BetfairProtocolFlows = {
    implicit val timeout: Timeout = Timeout(10.seconds)

    val protocolActor: ActorRef[BetfairProtocolMessage] = system.systemActorOf(
      BetfairProtocolActor(applicationKey, sessionToken),
      name = s"betfair-socket-protocol-${RandomUtils.generateRandomString()}",
      Props.empty
    )

    val incoming: IncomingFlow =
      ActorFlow
        .ask[IncomingBetfairSocketMessage, IncomingQuestion, Answer](protocolActor)((elem, replyTo) => IncomingQuestion(elem, replyTo))
        .mapConcat(_.messages)

    val outgoing: OutgoingFlow =
      ActorFlow
        .ask[OutgoingBetfairSocketMessage, OutgoingQuestion, Answer](protocolActor)(OutgoingQuestion(_, _))
        .mapConcat(_.messages)

    new BetfairProtocolFlows(incoming, outgoing)
  }
}

private object BetfairProtocolActor {

  sealed trait BetfairProtocolMessage
  final case class IncomingQuestion(incoming: IncomingBetfairSocketMessage, replyTo: ActorRef[Answer])
      extends BetfairProtocolMessage
  final case class OutgoingQuestion(outgoing: OutgoingBetfairSocketMessage, replyTo: ActorRef[Answer])
      extends BetfairProtocolMessage

  final case class Answer(messages: List[BetfairSocketMessage])
  object Answer {
    def apply(): Answer = Answer(List.empty)
  }

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
              replyTo ! Answer(List(Authentication(applicationKey, sessionToken)))
              authenticating()

            case message =>
              buffer.stash(message)
              Behaviors.same
          }

        def authenticating(): Behavior[BetfairProtocolMessage] = {
          context.system.eventStream ! Publish(SocketConnected)

          Behaviors.receiveMessagePartial[BetfairProtocolMessage] {
            case IncomingQuestion(Success(_), replyTo) =>
              context.system.log.info(
                s"[connecting] authentication success, moving to connected [unstashing ${buffer.size} messages]"
              )
              replyTo ! Answer()
              buffer.unstashAll(connected())

            case IncomingQuestion(Failure(_, _, _, _), replyTo) =>
              context.system.log.info(s"[connecting] authentication failure, stopping")
              context.system.eventStream ! Publish(SocketAuthenticationFailed)
              replyTo ! Answer(List(SocketFailed))
              Behaviors.stopped

            case message =>
              buffer.stash(message)
              Behaviors.same
          }
        }

        def connected(): Behavior[BetfairProtocolMessage] = {
          context.system.eventStream ! Publish(SocketAuthenticated)

          def handle(message: IncomingBetfairSocketMessage): List[IncomingBetfairSocketMessage] =
            message match {
              case mcm: MarketChangeMessage => List(mcm)
              case _                        => List.empty
            }

          Behaviors.receiveMessagePartial[BetfairProtocolMessage] {
            case IncomingQuestion(message, replyTo) =>
              context.system.log.info(s"[connected] receiving incoming message $message")
              replyTo ! Answer(handle(message))
              Behaviors.same

            case OutgoingQuestion(message, replyTo) =>
              context.system.log.info(s"[connected] receiving outgoing message $message")
              message match {
                case MarketSubscription(_, Some(marketFilter)) =>
                  context.system.eventStream ! Publish(MarketFilterUpdate(marketFilter))
                case _ => ()
              }
              replyTo ! Answer(List(message))
              Behaviors.same
          }
        }

        unconnected(sessionToken)
      }
    }
}

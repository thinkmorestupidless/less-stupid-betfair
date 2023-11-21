package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.streams.domain.{Heartbeat, OutgoingBetfairSocketMessage}
import com.thinkmorestupidless.betfair.streams.impl.BetfairHeartbeatActor.{
  Complete,
  Fail,
  HeartbeatMessage,
  OutgoingMessage
}
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import org.apache.pekko.stream.BoundedSourceQueue
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import org.apache.pekko.stream.typed.scaladsl.ActorSink

import scala.concurrent.duration._

object BetfairHeartbeatFlow {

  def apply(queue: BoundedSourceQueue[OutgoingBetfairSocketMessage], source: Source[OutgoingBetfairSocketMessage, NotUsed])(implicit
      system: ActorSystem[_]
  ): Flow[OutgoingBetfairSocketMessage, OutgoingBetfairSocketMessage, NotUsed] = {
    val actorRef = system.systemActorOf[HeartbeatMessage](BetfairHeartbeatActor(queue), "betfair-stream-heartbeat")
    val sink: Sink[OutgoingBetfairSocketMessage, NotUsed] = Flow[OutgoingBetfairSocketMessage]
      .map(OutgoingMessage(_))
      .to(ActorSink.actorRef[HeartbeatMessage](actorRef, Complete, Fail(_)))

    Flow.fromSinkAndSourceCoupled(sink, source)
  }
}

object BetfairHeartbeatActor {

  private val HeartbeatTimer = "Heartbeat"

  sealed trait HeartbeatMessage
  final case class OutgoingMessage(outgoing: OutgoingBetfairSocketMessage) extends HeartbeatMessage
  case object SendHeartbeat extends HeartbeatMessage
  case object Complete extends HeartbeatMessage
  final case class Fail(cause: Throwable) extends HeartbeatMessage

  def apply(queue: BoundedSourceQueue[OutgoingBetfairSocketMessage]): Behavior[HeartbeatMessage] =
    Behaviors.setup { _ =>
      Behaviors.withTimers { timers =>
        Behaviors.receiveMessage {
          case OutgoingMessage(_) =>
            if (timers.isTimerActive(HeartbeatTimer)) {
              timers.cancel(HeartbeatTimer)
            }
            timers.startSingleTimer(HeartbeatTimer, SendHeartbeat, 5.second)
            Behaviors.same

          case SendHeartbeat =>
            queue.offer(Heartbeat())
            timers.startSingleTimer(HeartbeatTimer, SendHeartbeat, 5.second)
            Behaviors.same

          case Complete =>
            queue.complete()
            Behaviors.stopped

          case Fail(cause) =>
            throw cause
        }
      }
    }
}

package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.core.domain.SocketAuthenticated
import com.thinkmorestupidless.betfair.streams.domain.{
  GlobalMarketFilterRepository,
  MarketFilter,
  MarketSubscription,
  OutgoingBetfairSocketMessage
}
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.eventstream.EventStream.Subscribe
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.BoundedSourceQueue
import com.thinkmorestupidless.betfair.streams.impl.MarketFilterUtils._

import scala.util.{Failure, Success}

object GlobalMarketSubscriptionSupplierActor {

  sealed trait Message
  case object GetGlobalMarketFilter extends Message
  final case class GlobalMarketFilterAvailable(globalMarketFilter: MarketFilter) extends Message
  final case class FailedToGetGlobalMarketFilter(cause: Throwable) extends Message

  def apply(
      globalMarketFilterRepository: GlobalMarketFilterRepository,
      queue: BoundedSourceQueue[OutgoingBetfairSocketMessage]
  ): Behavior[Message] =
    Behaviors.setup { context =>
      val eventStreamAdaptor = context.messageAdapter[SocketAuthenticated.type](_ => GetGlobalMarketFilter)
      context.system.eventStream ! Subscribe(eventStreamAdaptor)

      Behaviors.receiveMessage {
        case GetGlobalMarketFilter =>
          context.log.info("socket authenticated, getting global market filter")
          context.pipeToSelf(globalMarketFilterRepository.getCurrentGlobalFilter()) {
            case Success(globalMarketFilter) => GlobalMarketFilterAvailable(globalMarketFilter)
            case Failure(exception)          => FailedToGetGlobalMarketFilter(exception)
          }
          Behaviors.same

        case GlobalMarketFilterAvailable(globalMarketFilter) =>
          context.log.info("global market filter available")
          if (globalMarketFilter.isNotEmpty()) {
            context.log.info("global market filter is not empty, using it as a market subscription")
            queue.offer(MarketSubscription(globalMarketFilter))
          } else {
            context.log.info("global market filter is empty, NOT using it as a market subscription")
          }
          context.log.info("global market filter actor is stopping")
          Behaviors.stopped

        case FailedToGetGlobalMarketFilter(cause) =>
          throw cause
      }
    }
}

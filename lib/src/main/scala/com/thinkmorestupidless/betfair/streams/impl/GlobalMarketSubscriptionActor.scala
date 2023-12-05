package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.core.domain.{BetfairLifecycleEvent, MarketFilterUpdate, SocketAuthenticated}
import com.thinkmorestupidless.betfair.streams.domain.{
  GlobalMarketFilterRepository,
  MarketFilter,
  MarketSubscription,
  OutgoingBetfairSocketMessage
}
import com.thinkmorestupidless.betfair.streams.impl.MarketFilterUtils._
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.eventstream.EventStream.Subscribe
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.BoundedSourceQueue

import scala.util.{Failure, Success}

object GlobalMarketSubscriptionActor {

  sealed trait Message
  case object GetGlobalMarketFilter extends Message
  final case class UpdateGlobalMarketFilter(marketFilter: MarketFilter) extends Message
  final case class GlobalMarketFilterAvailable(globalMarketFilter: MarketFilter) extends Message
  final case class FailedToGetGlobalMarketFilter(cause: Throwable) extends Message
  case object GlobalMarketFilterUpdated extends Message
  final case class FailedToUpdateGlobalMarketFilter(cause: Throwable) extends Message
  case object IgnoredSocketEvent extends Message

  def apply(
      globalMarketFilterRepository: GlobalMarketFilterRepository,
      queue: BoundedSourceQueue[OutgoingBetfairSocketMessage]
  ): Behavior[Message] =
    Behaviors.setup { context =>
      val eventStreamAdaptor = context.messageAdapter[BetfairLifecycleEvent] {
        case SocketAuthenticated              => GetGlobalMarketFilter
        case MarketFilterUpdate(marketFilter) => UpdateGlobalMarketFilter(marketFilter)
        case _                                => IgnoredSocketEvent
      }
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
          Behaviors.same

        case UpdateGlobalMarketFilter(marketFilter) =>
          context.pipeToSelf(globalMarketFilterRepository.upsertGlobalMarketFilter(marketFilter)) {
            case Success(_)     => GlobalMarketFilterUpdated
            case Failure(cause) => FailedToUpdateGlobalMarketFilter(cause)
          }
          Behaviors.same

        case FailedToGetGlobalMarketFilter(cause) =>
          context.system.log.error("failed to updated global market filter", cause)
          throw cause

        case GlobalMarketFilterUpdated =>
          context.system.log.info("global market filter updated")
          Behaviors.same

        case FailedToUpdateGlobalMarketFilter(cause) =>
          context.system.log.error("failed to update global market filter", cause)
          throw cause

        case IgnoredSocketEvent =>
          Behaviors.same
      }
    }
}

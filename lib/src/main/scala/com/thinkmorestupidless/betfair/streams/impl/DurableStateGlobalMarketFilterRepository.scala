package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.streams.domain.{GlobalMarketFilterRepository, MarketFilter}
import com.thinkmorestupidless.betfair.streams.impl.GlobalMarketFilterActor.{GetGlobalMarketFilter, Message, UpdateGlobalMarketFilter}
import com.thinkmorestupidless.betfair.streams.impl.MarketFilterUtils._
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import org.apache.pekko.cluster.typed.{ClusterSingleton, SingletonActor}
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}
import org.apache.pekko.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

final class DurableStateGlobalMarketFilterRepository(proxy: ActorRef[Message])(implicit system: ActorSystem[_], ec: ExecutionContext) extends GlobalMarketFilterRepository {

  private implicit val timeout = Timeout(2.seconds)

  override def upsertGlobalMarketFilter(marketFilter: MarketFilter): Future[Unit] =
    proxy.ask(replyTo => UpdateGlobalMarketFilter(marketFilter, replyTo)).map(_ => ())

  override def getCurrentGlobalFilter(): Future[MarketFilter] = ???
    proxy.ask(replyTo => GetGlobalMarketFilter(replyTo))
}

object DurableStateGlobalMarketFilterRepository {

  def apply()(implicit system: ActorSystem[_]) = {
    val singletonManager = ClusterSingleton(system)
    val proxy: ActorRef[Message] = singletonManager.init(
      SingletonActor(Behaviors.supervise(GlobalMarketFilterActor()).onFailure[Exception](SupervisorStrategy.restart), "GlobalMarketFilter"))
  }
}

object GlobalMarketFilterActor {

  sealed trait Message
  final case class GetGlobalMarketFilter(replyTo: ActorRef[MarketFilter]) extends Message
  final case class UpdateGlobalMarketFilter(marketFilter: MarketFilter, replyTo: ActorRef[Done]) extends Message

  private val commandHandler: (MarketFilter, Message) => Effect[MarketFilter] =
    (marketFilter, message) =>
      message match {
        case GetGlobalMarketFilter(replyTo) => Effect.reply(replyTo)(marketFilter)
        case UpdateGlobalMarketFilter(newMarketFilter, replyTo) => Effect.persist(marketFilter.mergeWith(newMarketFilter)).thenReply(replyTo)(_ => Done)
      }

  def apply()(implicit system: ActorSystem[_]): Behavior[Message] =
    DurableStateBehavior[Message, MarketFilter](PersistenceId.ofUniqueId("GlobalMarketFilter"), MarketFilter.empty, commandHandler)
}

package com.thinkmorestupidless.betfair.navigation.impl

import cats.data.EitherT
import cats.syntax.either._
import com.thinkmorestupidless.betfair.navigation.domain.BetfairNavigationService.{
  NavigationServiceError,
  UnexpectedNavigationError
}
import com.thinkmorestupidless.betfair.navigation.domain.{BetfairNavigationService, Menu}
import com.thinkmorestupidless.betfair.navigation.impl.ClusterSingletonBetfairNavigationBehaviour._
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import org.apache.pekko.cluster.typed.{ClusterSingleton, SingletonActor}
import org.apache.pekko.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

final class ClusterSingletonBetfairNavigationService(singleton: ActorRef[Message])(implicit system: ActorSystem[_])
    extends BetfairNavigationService {

  private implicit val ec = system.executionContext
  private implicit val timeout = Timeout(10.seconds)

  override def menu(): EitherT[Future, BetfairNavigationService.NavigationServiceError, Menu] =
    EitherT(singleton.ask(replyTo => GetMenu(replyTo)).map {
      case MenuReply(menu)            => menu.asRight
      case ErrorGettingMenu(error, _) => error.asLeft
      case FailedToGetMenu(cause, _)  => UnexpectedNavigationError(cause).asLeft
    })
}

object ClusterSingletonBetfairNavigationService {

  def apply(
      navigationService: BetfairNavigationService
  )(implicit system: ActorSystem[_]): ClusterSingletonBetfairNavigationService = {
    val singleton = ClusterSingletonSessionTokenStoreActor.create(navigationService)
    new ClusterSingletonBetfairNavigationService(singleton)
  }
}

private object ClusterSingletonSessionTokenStoreActor {

  def create(navigationService: BetfairNavigationService)(implicit
      system: ActorSystem[_]
  ): ActorRef[Message] =
    ClusterSingleton(system).init(
      SingletonActor(
        Behaviors
          .supervise(ClusterSingletonBetfairNavigationBehaviour(navigationService))
          .onFailure[Exception](SupervisorStrategy.restart),
        "NavigationService"
      )
    )
}

object ClusterSingletonBetfairNavigationBehaviour {

  sealed trait Message
  final case class GetMenu(replyTo: ActorRef[Reply]) extends Message
  final case class MenuAvailable(menu: Menu, replyTo: ActorRef[Reply]) extends Message

  sealed trait Reply
  final case class MenuReply(menu: Menu) extends Reply
  final case class ErrorGettingMenu(error: NavigationServiceError, replyTo: ActorRef[Reply]) extends Message with Reply
  final case class FailedToGetMenu(cause: Throwable, replyTo: ActorRef[Reply]) extends Message with Reply

  def apply(navigationService: BetfairNavigationService): Behavior[Message] =
    Behaviors.setup { context =>
      Behaviors.withStash(capacity = 100) { buffer =>
        def running(): Behavior[Message] =
          Behaviors.receiveMessagePartial { case GetMenu(replyTo) =>
            context.pipeToSelf(navigationService.menu().value) {
              case Success(response) =>
                response match {
                  case Right(menu) => MenuAvailable(menu, replyTo)
                  case Left(error) => ErrorGettingMenu(error, replyTo)
                }
              case Failure(exception) => FailedToGetMenu(exception, replyTo)
            }
            waitingForMenu()
          }

        def waitingForMenu(): Behavior[Message] =
          Behaviors.receiveMessage {
            case getMenu: GetMenu =>
              buffer.stash(getMenu)
              Behaviors.same

            case MenuAvailable(menu, replyTo) =>
              replyTo ! MenuReply(menu)
              buffer.unstashAll(running())

            case errorGettingMenu: ErrorGettingMenu =>
              errorGettingMenu.replyTo ! errorGettingMenu
              buffer.unstashAll(running())

            case failedToGetMenu: FailedToGetMenu =>
              failedToGetMenu.replyTo ! failedToGetMenu
              buffer.unstashAll(running())
          }

        running()
      }
    }
}

package com.thinkmorestupidless.betfair.auth.impl

import cats.data.EitherT
import com.thinkmorestupidless.betfair.auth.domain.{BetfairAuthenticationService, SessionToken}
import com.thinkmorestupidless.betfair.auth.impl.SessionStoreBehaviour.{GetSessionToken, Message}
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import org.apache.pekko.cluster.typed.{ClusterSingleton, SingletonActor}
import org.apache.pekko.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final class ClusterSingletonBetfairAuthenticationService private (singleton: ActorRef[Message])(implicit
    system: ActorSystem[_]
) extends BetfairAuthenticationService {

  private implicit val ec = system.executionContext
  private implicit val timeout = Timeout(10.seconds)

  override def login(): EitherT[Future, BetfairAuthenticationService.AuthenticationError, SessionToken] =
    EitherT.liftF(singleton.ask(replyTo => GetSessionToken(replyTo)))
}

object ClusterSingletonBetfairAuthenticationService {

  def apply(
      authService: BetfairAuthenticationService
  )(implicit system: ActorSystem[_]): ClusterSingletonBetfairAuthenticationService = {
    val singleton = ClusterSingletonSessionTokenStoreActor.create(authService, NoOpSessionTokenTokenStore)
    new ClusterSingletonBetfairAuthenticationService(singleton)
  }
}

private object ClusterSingletonSessionTokenStoreActor {

  def create(authService: BetfairAuthenticationService, sessionTokenStore: SessionTokenStore)(implicit
      system: ActorSystem[_]
  ): ActorRef[Message] =
    ClusterSingleton(system).init(
      SingletonActor(
        Behaviors
          .supervise(SessionStoreBehaviour(authService, sessionTokenStore))
          .onFailure[Exception](SupervisorStrategy.restart),
        "SessionStore"
      )
    )
}

private object SessionStoreBehaviour {

  sealed trait Message
  final case class GetSessionToken(replyTo: ActorRef[SessionToken]) extends Message
  final case class SetStoredSessionToken(maybeSessionToken: Option[SessionToken]) extends Message
  final case class SetSessionTokenAndReply(sessionToken: SessionToken, replyTo: ActorRef[SessionToken]) extends Message
  final case class FailedToAuthenticate(error: Throwable) extends Message
  final case class FailedToRetrieveStoredSessionToken(error: Throwable) extends Message

  private val log = LoggerFactory.getLogger(getClass)

  def apply(authService: BetfairAuthenticationService, sessionTokenStore: SessionTokenStore): Behavior[Message] =
    Behaviors.setup { context =>
      Behaviors.withStash(capacity = 100) { buffer =>
        implicit val ec: ExecutionContext = context.executionContext

        def getToken(maybeSessionToken: Option[SessionToken]): Future[SessionToken] =
          maybeSessionToken.map(token => Future.successful(token)).getOrElse(authenticate())

        def authenticate(): Future[SessionToken] =
          authService.login().value.flatMap {
            case Right(sessionToken) => Future.successful(sessionToken)
            case Left(error)         => Future.failed(new RuntimeException(s"Failed to login to Betfair [$error]"))
          }

        def running(maybeSessionToken: Option[SessionToken]): Behavior[Message] =
          Behaviors.receiveMessagePartial { case GetSessionToken(replyTo) =>
            context.pipeToSelf(getToken(maybeSessionToken)) {
              case Success(sessionToken) => SetSessionTokenAndReply(sessionToken, replyTo)
              case Failure(exception)    => FailedToAuthenticate(exception)
            }
            whileGettingToken()
          }

        def whileGettingToken(): Behavior[Message] =
          Behaviors.receiveMessage {
            case getSessionToken: GetSessionToken =>
              buffer.stash(getSessionToken)
              Behaviors.same

            case SetStoredSessionToken(maybeSessionToken) =>
              running(maybeSessionToken)

            case SetSessionTokenAndReply(sessionToken, replyTo) =>
              replyTo ! sessionToken
              buffer.unstashAll(running(Some(sessionToken)))

            case FailedToAuthenticate(error) =>
              log.error(s"Failed to authenticate with Betfair caused by $error, shutting down")
              Behaviors.stopped

            case FailedToRetrieveStoredSessionToken(error) =>
              log.error(s"Failed to retrieve stored session token caused by $error, shutting down")
              Behaviors.stopped
          }

        context.pipeToSelf(sessionTokenStore.read()) {
          case Success(maybeSessionToken) => SetStoredSessionToken(maybeSessionToken)
          case Failure(exception)         => FailedToRetrieveStoredSessionToken(exception)
        }

        whileGettingToken()
      }
    }
}

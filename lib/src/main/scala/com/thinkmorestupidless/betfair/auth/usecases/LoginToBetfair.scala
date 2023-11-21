package com.thinkmorestupidless.betfair.auth.usecases

import cats.data.EitherT
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.AuthenticationError
import com.thinkmorestupidless.betfair.auth.domain.{BetfairAuthenticationService, SessionToken}
import com.thinkmorestupidless.betfair.core.domain.{Authenticated, AuthenticationFailed}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.eventstream.EventStream.Publish

import scala.concurrent.Future

object LoginToBetfair {

  type LoginToBetfair = () => EitherT[Future, AuthenticationError, SessionToken]

  def apply(authenticationService: BetfairAuthenticationService)(implicit system: ActorSystem[_]): LoginToBetfair = {
    implicit val ec = system.executionContext
    () => authenticationService.login().biSemiflatMap(onFailure, onSuccess)
  }

  private def onSuccess(sessionToken: SessionToken)(implicit system: ActorSystem[_]): Future[SessionToken] = {
    system.eventStream ! Publish(Authenticated)
    Future.successful(sessionToken)
  }

  private def onFailure(error: AuthenticationError)(implicit system: ActorSystem[_]): Future[AuthenticationError] = {
    system.eventStream ! Publish(AuthenticationFailed(error))
    Future.successful(error)
  }
}

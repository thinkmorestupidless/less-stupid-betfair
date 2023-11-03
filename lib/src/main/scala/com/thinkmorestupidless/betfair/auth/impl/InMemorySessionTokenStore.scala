package com.thinkmorestupidless.betfair.auth.impl

import com.thinkmorestupidless.betfair.auth.domain.SessionToken

import scala.concurrent.Future

object InMemorySessionTokenStore extends SessionTokenStore {

  private var maybeSessionToken: Option[SessionToken] = None

  override def read(): Future[Option[SessionToken]] = Future.successful(maybeSessionToken)
  override def write(sessionToken: SessionToken): Unit = maybeSessionToken = Some(sessionToken)
}

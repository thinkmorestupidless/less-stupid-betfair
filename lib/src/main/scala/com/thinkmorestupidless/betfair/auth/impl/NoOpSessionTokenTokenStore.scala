package com.thinkmorestupidless.betfair.auth.impl

import com.thinkmorestupidless.betfair.auth.domain.SessionToken

import scala.concurrent.Future

object NoOpSessionTokenTokenStore extends SessionTokenStore {

  override def read(): Future[Option[SessionToken]] = Future.successful(None)
  override def write(sessionToken: SessionToken): Unit = ()
}

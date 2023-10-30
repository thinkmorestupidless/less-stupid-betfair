package com.thinkmorestupidless.betfair.auth.impl

import com.thinkmorestupidless.betfair.auth.domain.{BetfairSession, SessionStore}

import scala.concurrent.Future

class NoOpSessioStore extends SessionStore {

  override def read(): Future[Option[BetfairSession]] =
    Future.successful(None)

  override def write(session: BetfairSession): Unit = ()
}

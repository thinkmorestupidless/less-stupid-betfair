package com.thinkmorestupidless.betfair.auth.domain

import scala.concurrent.Future

trait SessionStore {

  def read(): Future[Option[BetfairSession]]

  def write(session: BetfairSession)
}

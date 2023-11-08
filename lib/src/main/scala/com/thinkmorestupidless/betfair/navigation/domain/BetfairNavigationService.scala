package com.thinkmorestupidless.betfair.navigation.domain

import com.thinkmorestupidless.betfair.auth.domain.BetfairSession

import scala.concurrent.Future

trait BetfairNavigationService {

  def menu()(implicit session: BetfairSession): Future[Menu]
}

package com.thinkmorestupidless.betfair.navigation.domain

import com.thinkmorestupidless.betfair.auth.domain.BetfairSession

import scala.concurrent.Future

trait NavigationService {

  def menu()(session: BetfairSession): Future[Menu]
}

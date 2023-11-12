package com.thinkmorestupidless.betfair.grpc

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.exchange.impl.AkkaHttpBetfairExchangeService
import com.thinkmorestupidless.betfair.navigation.domain.usecases.GetMenuUseCase
import com.thinkmorestupidless.betfair.navigation.impl.PlayWsBetfairNavigationService
import org.apache.pekko.actor.ActorSystem

object DefaultBetfairGrpcServer {

  def apply(betfair: Betfair)(implicit system: ActorSystem): BetfairGrpcServer = {
    implicit val ec = system.dispatcher
    implicit val session = betfair.session

    val betfairNavigationService = new PlayWsBetfairNavigationService(betfair.config)
    val getMenuUseCase = GetMenuUseCase(betfairNavigationService)

    val betfairExchangeService = new AkkaHttpBetfairExchangeService(betfair.config)

    new BetfairGrpcServer(getMenuUseCase, betfairExchangeService)
  }
}

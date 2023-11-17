package com.thinkmorestupidless.betfair.grpc

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.exchange.impl.AkkaHttpBetfairExchangeService
import com.thinkmorestupidless.betfair.exchange.usecases.{ListEventTypesUseCase, ListEventsUseCase}
import com.thinkmorestupidless.betfair.navigation.impl.PlayWsBetfairNavigationService
import com.thinkmorestupidless.betfair.navigation.usecases.GetMenuUseCase
import org.apache.pekko.actor.ActorSystem

object DefaultBetfairGrpcServer {

  def apply(betfair: Betfair)(implicit system: ActorSystem): BetfairGrpcServer = {
    implicit val ec = system.dispatcher

    val getMenuUseCase = GetMenuUseCase(betfair.navigation)

    val listEventTypesUseCase = ListEventTypesUseCase(betfair.exchange)
    val listEventsUseCase = ListEventsUseCase(betfair.exchange)

    new BetfairGrpcServer(getMenuUseCase, listEventTypesUseCase, listEventsUseCase)
  }
}

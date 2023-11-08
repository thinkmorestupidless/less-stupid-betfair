package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.exchange.impl.AkkaHttpBetfairExchangeService
import com.thinkmorestupidless.betfair.grpc.BetfairGrpcServer
import com.thinkmorestupidless.betfair.navigation.domain.usecases.GetMenuUseCase
import com.thinkmorestupidless.betfair.navigation.impl.PlayWsBetfairNavigationService
import org.apache.pekko.actor.ActorSystem
import org.slf4j.LoggerFactory

object GrpcExample {

  private var log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.info("gRPC example starting")

    implicit val system = ActorSystem("grpc-example")
    implicit val ec = system.dispatcher

    Betfair()
      .map { betfair =>
        log.info("betfair is ready {}", betfair)

        implicit val session = betfair.session

        val betfairNavigationService = new PlayWsBetfairNavigationService(betfair.config)
        val getMenuUseCase = GetMenuUseCase(betfairNavigationService)

        val betfairExchangeService = new AkkaHttpBetfairExchangeService(betfair.config)

        val binding = new BetfairGrpcServer(getMenuUseCase, betfairExchangeService)

      }
      .leftMap(error => log.error(s"failed to log in to Betfair '$error'"))
  }
}

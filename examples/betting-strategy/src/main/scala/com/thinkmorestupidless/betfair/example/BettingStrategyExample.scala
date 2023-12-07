package com.thinkmorestupidless.betfair.example

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.navigation.domain.EventName.EnglishPremierLeague
import com.thinkmorestupidless.betfair.navigation.domain.MarketType.MatchOdds
import com.thinkmorestupidless.betfair.navigation.impl.MenuUtils._
import com.thinkmorestupidless.betfair.strategy.impl.{ActorBettingStrategyOrchestrator, LoggingBettingStrategy}
import com.thinkmorestupidless.betfair.streams.impl.MarketFilterUtils._
import com.thinkmorestupidless.betfair.streams.marketdefinitions.impl.InMemoryMarketDefinitionsRepository
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory

import java.time.Clock

object BettingStrategyExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.info("betting strategy example starting")

    implicit val system = ActorSystem(Behaviors.ignore, "betting-strategy-example")
    implicit val ec = system.executionContext
    implicit val clock = Clock.systemUTC()

    Betfair
      .create()
      .map { betfair =>
        log.info("betfair is ready {}", betfair)

        val strategyOrchestrator = ActorBettingStrategyOrchestrator(betfair, new InMemoryMarketDefinitionsRepository())

        betfair.getMenu().map { menu =>
          val premierLeagueMatchOddsMarketsFilter =
            menu.allEvents().ofType(EnglishPremierLeague).allMarkets().ofType(MatchOdds).toMarketFilter()

          val loggingBettingStrategy = LoggingBettingStrategy(premierLeagueMatchOddsMarketsFilter)
          strategyOrchestrator.registerBettingStrategy(loggingBettingStrategy)
        }
      }
      .leftMap(error => log.error(s"failed to log in to Betfair '$error'"))
  }
}

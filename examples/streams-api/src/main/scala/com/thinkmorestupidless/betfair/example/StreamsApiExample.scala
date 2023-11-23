package com.thinkmorestupidless.betfair.example

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.navigation.domain.EventName.EnglishPremierLeague
import com.thinkmorestupidless.betfair.navigation.domain.MarketType.MatchOdds
import com.thinkmorestupidless.betfair.navigation.impl.MenuUtils._
import com.thinkmorestupidless.betfair.streams.domain.{IncomingBetfairSocketMessage, MarketChange}
import com.thinkmorestupidless.betfair.streams.impl.MarketFilterUtils._
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.scaladsl.Sink
import org.slf4j.LoggerFactory

import java.time.Clock

object StreamsApiExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.info("streams api example starting")

    implicit val system = ActorSystem(Behaviors.ignore, "streams-api-example")
    implicit val ec = system.executionContext
    implicit val clock = Clock.systemUTC()

    Betfair
      .create()
      .map { betfair =>
        log.info("betfair is ready {}", betfair)

        betfair.getMenu().map { menu =>
          val marketSubscription = menu.allEvents.ofType(EnglishPremierLeague).allMarkets.ofType(MatchOdds).toMarketSubscription()
          betfair.subscribeToMarketChanges(marketSubscription, Sink.foreach[MarketChange](msg => log.info(msg.toString)))
        }
      }
      .leftMap(error => log.error(s"failed to log in to Betfair '$error'"))
  }
}

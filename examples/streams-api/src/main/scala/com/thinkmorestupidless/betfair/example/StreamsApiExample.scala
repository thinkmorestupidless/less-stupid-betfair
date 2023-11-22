package com.thinkmorestupidless.betfair.example

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.navigation.domain.{EventNames, MarketTypes}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory
import com.thinkmorestupidless.betfair.navigation.impl.MenuUtils._
import com.thinkmorestupidless.betfair.proto.streams.{MarketFilter => MarketFilterProto}
import com.thinkmorestupidless.betfair.streams.domain.{MarketFilter, MarketId, MarketSubscription, OutgoingBetfairSocketMessage}
import org.apache.pekko.stream.scaladsl.{Source, SourceQueueWithComplete}

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
          val soccerMatchOdds = menu.allEvents
            .filter(_.name == EventNames.EnglishPremierLeague)
            .flatMap(_.allMarkets)
            .filter(_.marketType == MarketTypes.MatchOdds)
            .map(_.id.value)
            .map(MarketId(_))
          val marketFilter = MarketFilter(soccerMatchOdds)
          log.info(s"subscribing to markets '$soccerMatchOdds'")
          val (queue, source) = Source.queue[OutgoingBetfairSocketMessage](bufferSize = 100).preMaterialize()

          source.runWith(betfair.socketFlow.sink)
          queue.offer(MarketSubscription(marketFilter))
        }
      }
      .leftMap(error => log.error(s"failed to log in to Betfair '$error'"))
  }
}

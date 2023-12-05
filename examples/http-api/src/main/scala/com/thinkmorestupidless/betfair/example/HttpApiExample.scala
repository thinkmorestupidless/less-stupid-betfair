package com.thinkmorestupidless.betfair.example

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.exchange.domain.{EventId, MarketFilter}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory

import java.time.Clock
import scala.concurrent.ExecutionContext

object HttpApiExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.info("streams api example starting")

    implicit val system = ActorSystem(Behaviors.ignore, "streams-api-example")
    implicit val ec = system.executionContext
    implicit val clock = Clock.systemUTC()

    Betfair.create().map { betfair =>
      log.info("betfair is ready")

      listEventTypes(betfair)
      listEvents(betfair, MarketFilter.empty.withEve)
    }
  }

  private def listEvents(betfair: Betfair, filter: MarketFilter)(implicit ec: ExecutionContext): Unit =
    betfair.listEvents(filter).map { eventResponse =>
      log.info(s"There are ${eventResponse.size} events")
      eventResponse.foreach(response =>
        log.info(s"${response.event.id} => ${response.event.name} has ${response.marketCount} markets")
      )
    }

  private def listEventTypes(betfair: Betfair)(implicit ec: ExecutionContext): Unit =
    betfair.listEventTypes(MarketFilter.empty).map { eventTypeResponse =>
      log.info(s"There are ${eventTypeResponse.size} event types")
      eventTypeResponse.foreach(response =>
        log.info(s"${response.eventType.id} => ${response.eventType.name} has ${response.marketCount} markets")
      )
    }
}

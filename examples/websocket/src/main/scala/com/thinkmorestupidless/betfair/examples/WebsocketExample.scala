package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.Betfair
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory

import java.time.Clock

object WebsocketExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.info("websocket example starting")

    implicit val system = ActorSystem(Behaviors.ignore, "grpc-example")
    implicit val ec = system.executionContext
    implicit val clock = Clock.systemUTC()

    Betfair.create()
      .map { betfair =>
        log.info("betfair is ready {}", betfair)
      }
      .leftMap(error => log.error(s"failed to log in to Betfair '$error'"))
  }
}

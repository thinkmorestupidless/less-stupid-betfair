package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.Betfair
import org.apache.pekko.actor.ActorSystem
import org.slf4j.LoggerFactory

object WebsocketExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.info("websocket example starting")

    implicit val system = ActorSystem("websocket-example")
    implicit val ec = system.dispatcher

    Betfair()
      .map { betfair =>
        log.info("betfair is ready {}", betfair)
      }
      .leftMap(error => log.error(s"failed to log in to Betfair '$error'"))
  }
}

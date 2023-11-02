package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.Betfair
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import org.apache.pekko.actor.ActorSystem
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._

object WebsocketExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.info("websocket example starting")
    println("starting")

    implicit val system = ActorSystem("websocket-example")
    implicit val ec = system.dispatcher

//    System.getProperties.asScala.map { case (k, v) => println(s"$k => $v") }

    Betfair().map { betfair =>
      log.info("betfair is ready {}", betfair)
    }
      .leftMap(error => log.error(s"failed to log in to Betfair '$error'"))
  }
}

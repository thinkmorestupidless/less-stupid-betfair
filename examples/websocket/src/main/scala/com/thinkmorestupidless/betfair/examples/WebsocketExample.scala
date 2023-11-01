package com.thinkmorestupidless.betfair.examples

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.thinkmorestupidless.betfair.Betfair
import org.slf4j.LoggerFactory

object WebsocketExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.info("websocket example starting")
    println("starting")

//    implicit val typedSystem = ActorSystem(Behaviors.ignore, "samples-websocket")
//    implicit val classicSystem = typedSystem.toClassic
//    implicit val ec = typedSystem.executionContext
//
//    Betfair().map { betfair =>
//      log.info("betfair is ready {}", betfair)
//    }
  }
}

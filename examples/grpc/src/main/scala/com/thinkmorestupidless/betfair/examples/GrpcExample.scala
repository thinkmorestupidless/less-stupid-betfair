package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.grpc.BetfairGrpcServer
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.slf4j.LoggerFactory

import java.time.Clock

object GrpcExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.ignore, "grpc-example")
    implicit val classicSystem = system.toClassic
    implicit val ec = system.executionContext
    implicit val clock = Clock.systemUTC()

    Betfair
      .create()
      .map(betfair => new BetfairGrpcServer(betfair).run())
      .leftMap(error => log.error(s"Something went wrong '$error'"))
  }
}

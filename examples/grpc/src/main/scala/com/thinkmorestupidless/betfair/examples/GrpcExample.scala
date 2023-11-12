package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.grpc.DefaultBetfairGrpcServer
import org.apache.pekko.actor.ActorSystem
import org.slf4j.LoggerFactory

object GrpcExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("grpc-example")
    implicit val ec = system.dispatcher

    Betfair()
      .map(betfair => DefaultBetfairGrpcServer(betfair).run())
      .leftMap(error => log.error(s"failed to log in to Betfair '$error'"))
  }
}

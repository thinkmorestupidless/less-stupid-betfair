package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.Betfair
import org.apache.pekko.actor.ActorSystem
import org.slf4j.LoggerFactory

object GrpcExample {

  private var log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.info("gRPC example starting")

    implicit val system = ActorSystem("grpc-example")
    implicit val ec = system.dispatcher

    Betfair().map { betfair =>
      log.info("betfair is ready {}", betfair)

      val binding = new GrpcExampleServer(system).run()

    }.leftMap(error => log.error(s"failed to log in to Betfair '$error'"))
  }
}

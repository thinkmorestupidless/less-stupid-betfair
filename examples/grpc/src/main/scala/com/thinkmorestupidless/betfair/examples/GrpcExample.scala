package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.grpc.BetfairGrpcServer
import com.thinkmorestupidless.betfair.proto.streams.{
  BetfairStreamsServiceClient,
  MarketFilter,
  SubscribeToMarketChangesRequest
}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.grpc.GrpcClientSettings
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.slf4j.LoggerFactory

import java.time.Clock
import scala.concurrent.duration._

object GrpcExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.ignore, "grpc-example")
//    implicit val classicSystem = system.toClassic
    implicit val ec = system.executionContext
    implicit val clock = Clock.systemUTC()

    Betfair
      .create()
      .map { betfair =>
        implicit val classicSystem = system.toClassic

        new BetfairGrpcServer(betfair).run()
        val settings =
          GrpcClientSettings.connectToServiceAt("localhost", 8080)(classicSystem).withDeadline(1.second).withTls(false)
        val client = BetfairStreamsServiceClient(settings)(classicSystem)

        log.info("invoking service client")

        val source =
          client.subscribeToMarketChanges().invoke(SubscribeToMarketChangesRequest(Some(MarketFilter.defaultInstance)))

        log.info(s"source is $source")

        implicit val mat = Materializer(classicSystem)

        source.runWith(Sink.foreach(println))
      }
      .leftMap(error => log.error(s"Something went wrong '$error'"))
  }
}

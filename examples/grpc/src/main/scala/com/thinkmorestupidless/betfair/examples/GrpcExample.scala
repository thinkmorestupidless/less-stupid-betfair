package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.grpc.BetfairGrpcServer
import com.thinkmorestupidless.betfair.navigation.domain.EventName.EnglishPremierLeague
import com.thinkmorestupidless.betfair.navigation.domain.MarketType.MatchOdds
import com.thinkmorestupidless.betfair.navigation.impl.MenuUtils._
import com.thinkmorestupidless.betfair.proto.streams.{
  BetfairStreamsServiceClient,
  MarketFilter => MarketFilterProto,
  SubscribeToMarketChangesRequest
}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.grpc.GrpcClientSettings
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory

import java.time.Clock
import scala.concurrent.duration._

object GrpcExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.ignore, "grpc-example")
    implicit val ec = system.executionContext
    implicit val clock = Clock.systemUTC()

    Betfair
      .create()
      .map { betfair =>
        implicit val classicSystem = system.toClassic

        new BetfairGrpcServer(betfair).run()

        val settings =
          GrpcClientSettings
            .connectToServiceAt("localhost", 8080)(classicSystem)
            .withDeadline(10.seconds)
            .withTls(false)
        val streamsClient = BetfairStreamsServiceClient(settings)(classicSystem)

        betfair.getMenu().map { menu =>
          val marketFilter =
            MarketFilterProto(
              menu.allEvents().ofType(EnglishPremierLeague).allMarkets().ofType(MatchOdds).map(_.id.value)
            )
          val source =
            streamsClient.subscribeToMarketChanges(SubscribeToMarketChangesRequest(Some(marketFilter))).recover {
              case e: Exception =>
                log.error("failed to create source", e)
                Source.empty
            }

          implicit val mat = Materializer(classicSystem)

          source.runWith(Sink.foreach(o => log.info(o.toString)))
        }

//        val x = betfair.listAllEventTypes().map { allEventTypes =>
//          log.info(s"event types: $allEventTypes")
//        }
//
//        log.info("invoking service client")

//
//        log.info(s"source is $source")
//

      }
      .leftMap(error => log.error(s"Something went wrong '$error'"))
  }
}

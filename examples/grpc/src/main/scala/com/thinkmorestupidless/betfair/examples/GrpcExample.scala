package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.grpc.BetfairGrpcServer
import com.thinkmorestupidless.betfair.navigation.domain.{EventNames, MarketTypes, Menu}
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
import com.thinkmorestupidless.betfair.navigation.impl.MenuUtils._
import com.thinkmorestupidless.betfair.proto.navigation.{
  BetfairNavigationServiceClient,
  GetMenuRequest,
  Menu => MenuProto
}
import com.thinkmorestupidless.betfair.streams.domain
import com.thinkmorestupidless.betfair.streams.domain.MarketId
import com.thinkmorestupidless.grpc.Encoder._
import com.thinkmorestupidless.betfair.streams.impl.grpc.Encoders._
import com.thinkmorestupidless.grpc.Decoder._
import com.thinkmorestupidless.betfair.navigation.impl.grpc.Decoders._

import java.time.Clock
import scala.concurrent.duration._

object GrpcExample {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.ignore, "grpc-example")
//    implicit val classicSystem = system.toClassic
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
        val navigationClient = BetfairNavigationServiceClient(settings)(classicSystem)

        val menu = navigationClient.getMenu(GetMenuRequest())

        betfair.getMenu().map { menu =>
          val soccerMatchOdds = menu.allEvents
            .filter(_.name == EventNames.EnglishPremierLeague)
            .flatMap(_.allMarkets)
            .filter(_.marketType == MarketTypes.MatchOdds)
            .map(_.id.value)
          val marketFilter = MarketFilterProto(soccerMatchOdds)
          log.info(s"subscribing to markets '$soccerMatchOdds'")
          val source =
            streamsClient.subscribeToMarketChanges(SubscribeToMarketChangesRequest(Some(marketFilter))).recover {
              case e: Exception =>
                log.error("failed to create source", e)
                Source.empty
            }

          implicit val mat = Materializer(classicSystem)

          source.runWith(Sink.foreach(println))
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

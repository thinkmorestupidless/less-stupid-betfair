package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.auth.impl.{InMemorySessionTokenStore, PlayWsBetfairAuthenticationService}
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.betfair.grpc.BetfairGrpcServer
import com.thinkmorestupidless.betfair.proto.streams.{
  BetfairStreamsServiceClient,
  MarketFilter,
  SubscribeToMarketChangesRequest
}
import com.thinkmorestupidless.betfair.streams.domain.{Heartbeat, OutgoingBetfairSocketMessage}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.{actor => classic}
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.grpc.GrpcClientSettings
import org.apache.pekko.stream.{ActorMaterializer, Materializer}
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import org.apache.pekko.util.ByteString
import org.slf4j.LoggerFactory

import java.time.Clock
import scala.concurrent.duration._

final class TlsSocketFlowSpec extends AnyWordSpecLike {

  val log = LoggerFactory.getLogger(getClass)
  val config: Config = ConfigFactory.load()
  val testKit: ActorTestKit = ActorTestKit(config)

  "apply" should {
    "so stuff" in {
      implicit val classic = testKit.system.classicSystem

      val betfairConfig =
        BetfairConfig.load().getOrElse(throw new RuntimeException("failed to load the betfair config"))

      val socketFlow = TlsSocketFlow.fromConfig(betfairConfig.exchange.socket)
      val codecFlow = BetfairCodecFlow().join(socketFlow)

      val applicationKey = betfairConfig.auth.credentials.applicationKey
      val authenticationService = PlayWsBetfairAuthenticationService(betfairConfig, InMemorySessionTokenStore)
      val globalMarketFilterRepository = InMemoryMarketFilterRepository()(classic.dispatcher)

      val protocolFlow = BetfairProtocolFlow(applicationKey, authenticationService, globalMarketFilterRepository)(
        testKit.system
      ).join(codecFlow)

      val flowUnderTest = Flow[OutgoingBetfairSocketMessage]
        .map { elem =>
          println(s"IN => $elem")
          elem
        }
        .via(protocolFlow)
        .map { elem =>
          println(s"OUT => $elem")
          elem
        }

      val sink = Sink.foreach(println)
      val source = Source.single(Heartbeat)

      source.via(flowUnderTest).to(sink).run()

//      implicit val classicSystem: classic.ActorSystem = testKit.system.classicSystem
//      implicit val typedSystem = testKit.system
//      implicit val ec = testKit.system.executionContext
//      implicit val clock = Clock.systemUTC()
//
//      Betfair
//        .create()
//        .map { betfair =>
//          new BetfairGrpcServer(betfair).run()
//          val settings = GrpcClientSettings
//            .connectToServiceAt("localhost", 8080)(testKit.system.classicSystem)
//            .withDeadline(1.second)
//            .withTls(false)
//          val client = BetfairStreamsServiceClient(settings)(testKit.system.classicSystem)
//
//          val source = client
//            .subscribeToMarketChanges()
//            .invoke(SubscribeToMarketChangesRequest(Some(MarketFilter.defaultInstance)))
//
//          implicit val mat = Materializer(classicSystem)
//
//          source.runWith(Sink.foreach(println))
//        }
//        .leftMap(error => log.error(s"Something went wrong '$error'"))

    }
  }
}

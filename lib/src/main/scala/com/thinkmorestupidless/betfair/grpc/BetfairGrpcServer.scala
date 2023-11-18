package com.thinkmorestupidless.betfair.grpc

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.exchange.impl.grpc.GprcExchangeService
import com.thinkmorestupidless.betfair.navigation.impl.grpc.GrpcNavigationServiceImpl
import com.thinkmorestupidless.betfair.proto.exchange.{ExchangeService, ExchangeServiceHandler}
import com.thinkmorestupidless.betfair.proto.navigation.{NavigationService, NavigationServiceHandler}
import com.thinkmorestupidless.betfair.proto.streams.{BetfairStreamsService, BetfairStreamsServiceHandler}
import com.thinkmorestupidless.betfair.streams.impl.grpc.GrpcStreamsServiceImpl
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.grpc.scaladsl.{ServerReflection, ServiceHandler}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final class BetfairGrpcServer(betfair: Betfair)(implicit system: ActorSystem) {

  private val log = LoggerFactory.getLogger(getClass)

  def run(): Future[Http.ServerBinding] = {
    implicit val ec: ExecutionContext = system.dispatcher

    val navigationPartial = NavigationServiceHandler.partial(new GrpcNavigationServiceImpl(betfair.getMenu))
    val exchangePartial =
      ExchangeServiceHandler.partial(new GprcExchangeService(betfair.listEventTypes, betfair.listEvents))
    val streamsPartial = BetfairStreamsServiceHandler.partial(new GrpcStreamsServiceImpl(betfair))

    val reflection = ServerReflection.partial(List(NavigationService, ExchangeService, BetfairStreamsService))

    val service: HttpRequest => Future[HttpResponse] =
      ServiceHandler.concatOrNotFound(navigationPartial, exchangePartial, streamsPartial, reflection)

    val bound: Future[Http.ServerBinding] = Http(system)
      .newServerAt(interface = "0.0.0.0", port = 8080)
      .bind(service)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        log.info(s"gRPC server bound to ${address.getHostString}:${address.getPort}")
      case Failure(ex) =>
        log.error("Failed to bind gRPC endpoint, terminating system", ex)
        system.terminate()
    }

    bound
  }
}

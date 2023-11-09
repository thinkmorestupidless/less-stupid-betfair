package com.thinkmorestupidless.betfair.grpc

import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService
import com.thinkmorestupidless.betfair.exchange.impl.grpc.GprcExchangeService
import com.thinkmorestupidless.betfair.navigation.domain.BetfairNavigationService
import com.thinkmorestupidless.betfair.navigation.domain.usecases.GetMenuUseCase
import com.thinkmorestupidless.betfair.navigation.domain.usecases.GetMenuUseCase.GetMenuUseCase
import com.thinkmorestupidless.betfair.navigation.impl.grpc.GprcNavigationService
import com.thinkmorestupidless.betfair.proto.exchange.{ExchangeService, ExchangeServiceHandler}
import com.thinkmorestupidless.betfair.proto.navigation.{NavigationService, NavigationServiceHandler}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.grpc.scaladsl.{ServerReflection, ServiceHandler}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final class BetfairGrpcServer(
    getMenuUseCase: GetMenuUseCase,
    betfairExchangeService: BetfairExchangeService
)(implicit system: ActorSystem) {

  private val log = LoggerFactory.getLogger(getClass)

  def run(): Future[Http.ServerBinding] = {
    implicit val ec: ExecutionContext = system.dispatcher

    val navigationPartial = NavigationServiceHandler.partial(new GprcNavigationService(getMenuUseCase))
    val exchangePartial = ExchangeServiceHandler.partial(new GprcExchangeService(betfairExchangeService))
    val reflection = ServerReflection.partial(List(NavigationService, ExchangeService))

    val service: HttpRequest => Future[HttpResponse] =
      ServiceHandler.concatOrNotFound(navigationPartial, exchangePartial, reflection)

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

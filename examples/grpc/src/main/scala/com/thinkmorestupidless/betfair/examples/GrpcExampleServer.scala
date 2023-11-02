package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.examples.grpc.GrpcExampleServiceHandler

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import pekko.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

final class GrpcExampleServer(system: ActorSystem) {

  private val log = LoggerFactory.getLogger(getClass)

  def run(): Future[Http.ServerBinding] = {
    implicit val sys = system
    implicit val ec: ExecutionContext = system.dispatcher

    val service: HttpRequest => Future[HttpResponse] =
      GrpcExampleServiceHandler(new GrpcExampleServiceImpl())

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

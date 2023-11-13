package com.thinkmorestupidless.utils

import org.apache.pekko.Done
import org.apache.pekko.actor.{ActorSystem, CoordinatedShutdown}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Route

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object HttpServer {

  def start(name: String, routes: Route, port: Int)(implicit system: ActorSystem): Unit = {

    // Akka-http is not yet typed, so we have to do some slight trickery to make it work with akka-typed.
    implicit val ex: ExecutionContext = system.dispatcher
    val shutdown = CoordinatedShutdown(system)

    Http().newServerAt(interface = "0.0.0.0", port).bind(routes).onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        val uri = s"http://${address.getHostString}:${address.getPort}"

        system.log.info(s"$name online at $uri")

        shutdown.addTask(CoordinatedShutdown.PhaseServiceRequestsDone, "http-graceful-terminate") { () =>
          binding.terminate(10.seconds).map { _ =>
            system.log.info(s"$name at $uri graceful shutdown completed")
            Done
          }
        }
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
}

package com.thinkmorestupidless.betfair

import cats.data.EitherT
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.AuthenticationError
import com.thinkmorestupidless.betfair.auth.domain.{BetfairSession, SessionToken}
import com.thinkmorestupidless.betfair.auth.impl.PlayWsBetfairAuthenticationService
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.betfair.exchange.domain._
import com.thinkmorestupidless.betfair.exchange.impl.AkkaHttpBetfairExchangeService
import org.apache.pekko.actor.ActorSystem
import pureconfig.error.ConfigReaderFailures

import scala.concurrent.{ExecutionContext, Future}

final case class Betfair(config: BetfairConfig, session: BetfairSession, exchange: BetfairExchangeService)

object Betfair {

  sealed trait BetfairError
  final case class FailedToLoadBetfairConfig(cause: ConfigReaderFailures) extends BetfairError
  final case class FailedBetfairAuthentication(cause: AuthenticationError) extends BetfairError

  def apply(system: ActorSystem): EitherT[Future, BetfairError, Betfair] = {
    implicit val sys = system
    implicit val ec = system.dispatcher

    this()
  }

  def apply()(implicit system: ActorSystem, ec: ExecutionContext): EitherT[Future, BetfairError, Betfair] =
    authenticate().map { case (config, session) =>
      val exchange = new AkkaHttpBetfairExchangeService(config)
      new Betfair(config, session, exchange)
    }

  private def authenticate()(implicit
      system: ActorSystem,
      ec: ExecutionContext
  ): EitherT[Future, BetfairError, (BetfairConfig, BetfairSession)] =
    for {
      config <- loadConfig()
      sessionToken <- authenticate(config)
    } yield (config, BetfairSession(config.auth.credentials.applicationKey, sessionToken))

  private def authenticate(
      config: BetfairConfig
  )(implicit system: ActorSystem, ec: ExecutionContext): EitherT[Future, BetfairError, SessionToken] = {
    val authenticator = PlayWsBetfairAuthenticationService(config)
    authenticator.login().leftMap(FailedBetfairAuthentication(_))
  }

  private def loadConfig()(implicit ec: ExecutionContext): EitherT[Future, BetfairError, BetfairConfig] =
    EitherT.fromEither[Future](BetfairConfig.load()).leftMap(FailedToLoadBetfairConfig(_))
}

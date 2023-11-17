package com.thinkmorestupidless.betfair

import cats.data.EitherT
import com.thinkmorestupidless.betfair.auth.impl.{ClusterSingletonBetfairAuthenticationService, PlayWsBetfairAuthenticationService, SessionTokenStore}
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.betfair.exchange.domain._
import com.thinkmorestupidless.betfair.exchange.impl.AkkaHttpBetfairExchangeService
import com.thinkmorestupidless.betfair.navigation.domain.BetfairNavigationService
import com.thinkmorestupidless.betfair.navigation.impl.{ClusterSingletonBetfairNavigationService, PlayWsBetfairNavigationService}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.adapter._
import pureconfig.error.ConfigReaderFailures

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

final case class Betfair(config: BetfairConfig, navigation: BetfairNavigationService, exchange: BetfairExchangeService)

object Betfair {

  sealed trait BetfairError
  final case class FailedToLoadBetfairConfig(cause: ConfigReaderFailures) extends BetfairError

  def create()(implicit clock: Clock, system: ActorSystem[_]): EitherT[Future, BetfairError, Betfair] = {
    implicit val ec = system.executionContext
    implicit val classicSystem = system.toClassic
    loadConfig().map { config =>
      val sessionTokenStore = SessionTokenStore.fromConfig(config.auth.sessionStore)
      val authenticationService = PlayWsBetfairAuthenticationService(config, sessionTokenStore)
      val navigationService = PlayWsBetfairNavigationService(config, authenticationService)
      val exchangeService = new AkkaHttpBetfairExchangeService(config, authenticationService)
      new Betfair(config, navigationService, exchangeService)
    }
  }

  def createClustered()(implicit clock: Clock, system: ActorSystem[_]): EitherT[Future, BetfairError, Betfair] = {
    implicit val ec = system.executionContext
    implicit val classicSystem = system.toClassic
    loadConfig().map { config =>
      val sessionTokenStore = SessionTokenStore.fromConfig(config.auth.sessionStore)
      val underlyingAuthenticationService = PlayWsBetfairAuthenticationService(config, sessionTokenStore)
      val authenticationService = ClusterSingletonBetfairAuthenticationService(underlyingAuthenticationService)

      val underlyingNavigationService = PlayWsBetfairNavigationService(config, authenticationService)
      val navigationService = ClusterSingletonBetfairNavigationService(underlyingNavigationService)

      val underlyingExchangeService = new AkkaHttpBetfairExchangeService(config, authenticationService)

      ???
    }
  }

  private def loadConfig()(implicit ec: ExecutionContext): EitherT[Future, BetfairError, BetfairConfig] =
    EitherT.fromEither[Future](BetfairConfig.load()).leftMap(FailedToLoadBetfairConfig(_))
}

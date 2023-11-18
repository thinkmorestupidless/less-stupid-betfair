package com.thinkmorestupidless.betfair

import cats.data.EitherT
import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, BetfairAuthenticationService}
import com.thinkmorestupidless.betfair.auth.impl.{
  ClusterSingletonBetfairAuthenticationService,
  PlayWsBetfairAuthenticationService,
  SessionTokenStore
}
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.betfair.exchange.domain._
import com.thinkmorestupidless.betfair.exchange.impl.{
  AkkaHttpBetfairExchangeService,
  ClusterSingletonBetfairExchangeService
}
import com.thinkmorestupidless.betfair.exchange.usecases.ListEventTypesUseCase.ListEventTypesUseCase
import com.thinkmorestupidless.betfair.exchange.usecases.ListEventsUseCase.ListEventsUseCase
import com.thinkmorestupidless.betfair.exchange.usecases.{ListEventTypesUseCase, ListEventsUseCase}
import com.thinkmorestupidless.betfair.navigation.domain.BetfairNavigationService
import com.thinkmorestupidless.betfair.navigation.impl.{
  ClusterSingletonBetfairNavigationService,
  PlayWsBetfairNavigationService
}
import com.thinkmorestupidless.betfair.navigation.usecases.GetMenuUseCase
import com.thinkmorestupidless.betfair.navigation.usecases.GetMenuUseCase.GetMenuUseCase
import com.thinkmorestupidless.betfair.streams.domain.GlobalMarketFilterRepository
import com.thinkmorestupidless.betfair.streams.impl.TlsSocketFlow.TlsSocketFlow
import com.thinkmorestupidless.betfair.streams.impl.{BetfairSocketFlow, InMemoryMarketFilterRepository, TlsSocketFlow}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.{actor => classic}
import pureconfig.error.ConfigReaderFailures

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

final case class Betfair(
    getMenu: GetMenuUseCase,
    listEventTypes: ListEventTypesUseCase,
    listEvents: ListEventsUseCase,
    socketFlow: BetfairSocketFlow
)

object Betfair {

  sealed trait BetfairError
  final case class FailedToLoadBetfairConfig(cause: ConfigReaderFailures) extends BetfairError

  def create(
      maybeSocketFlow: Option[TlsSocketFlow] = None,
      maybeGlobalMarketFilterRepository: Option[GlobalMarketFilterRepository] = None
  )(implicit clock: Clock, system: ActorSystem[_]): EitherT[Future, BetfairError, Betfair] = {
    implicit val ec = system.executionContext
    implicit val classicSystem = system.toClassic
    loadConfig().map { config =>
      val authenticationService = createAuthenticationService(config)
      val (navigationService, exchangeService) = createUnderlyingServices(config, authenticationService)
      val applicationKey = config.auth.credentials.applicationKey
      val globalMarketFilterRepository = maybeGlobalMarketFilterRepository.getOrElse(InMemoryMarketFilterRepository())
      val socketFlow = maybeSocketFlow.getOrElse(TlsSocketFlow(config.exchange.socket))

      create(
        authenticationService,
        navigationService,
        exchangeService,
        applicationKey,
        socketFlow,
        globalMarketFilterRepository
      )
    }
  }

  def createClustered(
      maybeSocketFlow: Option[TlsSocketFlow] = None,
      maybeGlobalMarketFilterRepository: Option[GlobalMarketFilterRepository] = None
  )(implicit clock: Clock, system: ActorSystem[_]): EitherT[Future, BetfairError, Betfair] = {
    implicit val ec = system.executionContext
    implicit val classicSystem = system.toClassic

    loadConfig().map { config =>
      val underlyingAuthenticationService = createAuthenticationService(config)
      val authenticationService = ClusterSingletonBetfairAuthenticationService(underlyingAuthenticationService)
      val (underlyingNavigationService, underlyingExchangeService) =
        createUnderlyingServices(config, authenticationService)
      val navigationService = ClusterSingletonBetfairNavigationService(underlyingNavigationService)
      val exchangeService = ClusterSingletonBetfairExchangeService(underlyingExchangeService)
      val applicationKey = config.auth.credentials.applicationKey
      val globalMarketFilterRepository = maybeGlobalMarketFilterRepository.getOrElse(InMemoryMarketFilterRepository())
      val socketFlow = maybeSocketFlow.getOrElse(TlsSocketFlow(config.exchange.socket))

      create(
        authenticationService,
        navigationService,
        exchangeService,
        applicationKey,
        socketFlow,
        globalMarketFilterRepository
      )
    }
  }

  private def create(
      authenticationService: BetfairAuthenticationService,
      navigationService: BetfairNavigationService,
      exchangeService: BetfairExchangeService,
      applicationKey: ApplicationKey,
      socketFlow: TlsSocketFlow.TlsSocketFlow,
      globalMarketFilterRepository: GlobalMarketFilterRepository
  )(implicit
      ec: ExecutionContext,
      system: ActorSystem[_]
  ): Betfair = {
    val getMenu = GetMenuUseCase(navigationService)

    val listEventTypes = ListEventTypesUseCase(exchangeService)
    val listEvents = ListEventsUseCase(exchangeService)

    val betfairSocketFlow =
      BetfairSocketFlow(socketFlow, applicationKey, authenticationService, globalMarketFilterRepository)

    Betfair(getMenu, listEventTypes, listEvents, betfairSocketFlow)
  }

  private def createAuthenticationService(
      config: BetfairConfig
  )(implicit clock: Clock, system: classic.ActorSystem, ec: ExecutionContext): BetfairAuthenticationService = {
    val sessionTokenStore = SessionTokenStore.fromConfig(config.auth.sessionStore)
    PlayWsBetfairAuthenticationService(config, sessionTokenStore)
  }

  private def createUnderlyingServices(config: BetfairConfig, authenticationService: BetfairAuthenticationService)(
      implicit system: ActorSystem[_]
  ): (BetfairNavigationService, BetfairExchangeService) = {
    implicit val ec = system.executionContext
    implicit val classicSystem = system.toClassic
    val navigationService = PlayWsBetfairNavigationService(config, authenticationService)
    val exchangeService = new AkkaHttpBetfairExchangeService(config, authenticationService)

    (navigationService, exchangeService)
  }

  private def loadConfig()(implicit ec: ExecutionContext): EitherT[Future, BetfairError, BetfairConfig] =
    EitherT.fromEither[Future](BetfairConfig.load()).leftMap(FailedToLoadBetfairConfig(_))
}

package com.thinkmorestupidless.betfair

import cats.data.EitherT
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.AuthenticationError
import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, BetfairAuthenticationService, SessionToken}
import com.thinkmorestupidless.betfair.auth.impl.{PlayWsBetfairAuthenticationService, SessionTokenStore}
import com.thinkmorestupidless.betfair.auth.usecases.LoginToBetfair
import com.thinkmorestupidless.betfair.core.impl.{BetfairConfig, SocketConfig}
import com.thinkmorestupidless.betfair.exchange.domain._
import com.thinkmorestupidless.betfair.exchange.impl.AkkaHttpBetfairExchangeService
import com.thinkmorestupidless.betfair.exchange.usecases.ListAllEventTypesUseCase.ListAllEventTypesUseCase
import com.thinkmorestupidless.betfair.exchange.usecases.ListEventTypesUseCase.ListEventTypesUseCase
import com.thinkmorestupidless.betfair.exchange.usecases.ListEventsUseCase.ListEventsUseCase
import com.thinkmorestupidless.betfair.exchange.usecases.{
  ListAllEventTypesUseCase,
  ListEventTypesUseCase,
  ListEventsUseCase
}
import com.thinkmorestupidless.betfair.navigation.domain.BetfairNavigationService
import com.thinkmorestupidless.betfair.navigation.impl.PlayWsBetfairNavigationService
import com.thinkmorestupidless.betfair.navigation.usecases.GetMenuUseCase
import com.thinkmorestupidless.betfair.navigation.usecases.GetMenuUseCase.GetMenuUseCase
import com.thinkmorestupidless.betfair.streams.domain.{
  GlobalMarketFilterRepository,
  MarketChange,
  MarketChangeMessage,
  MarketSubscription
}
import com.thinkmorestupidless.betfair.streams.impl.TlsSocketFlow.TlsSocketFlow
import com.thinkmorestupidless.betfair.streams.impl.{BetfairSocket, InMemoryMarketFilterRepository, TlsSocketFlow}
import com.thinkmorestupidless.betfair.streams.marketdefinitions.domain.MarketDefinitionsRepository
import com.thinkmorestupidless.betfair.streams.marketdefinitions.impl.InMemoryMarketDefinitionsRepository
import com.thinkmorestupidless.utils.EitherTUtils._
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.{actor => classic}
import pureconfig.error.ConfigReaderFailures

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

final case class Betfair(
    getMenu: GetMenuUseCase,
    listAllEventTypes: ListAllEventTypesUseCase,
    listEventTypes: ListEventTypesUseCase,
    listEvents: ListEventsUseCase,
    socketFlow: BetfairSocket
) {
  def subscribeToMarketChanges[T](marketSubscription: MarketSubscription, sink: Sink[MarketChange, T])(implicit
      mat: Materializer
  ): T = {
    socketFlow.marketSubscriptionQueue.offer(marketSubscription)
    socketFlow.source
      .collect { case mcm: MarketChangeMessage =>
        mcm.mc
      }
      .mapConcat(identity)
      .runWith(sink)
  }
}

object Betfair {

  sealed trait BetfairError
  final case class FailedToLoadBetfairConfig(cause: ConfigReaderFailures) extends BetfairError
  final case class FailedToLoginToBetfair(error: AuthenticationError) extends BetfairError

  def create(
      maybeSocketFlow: Option[TlsSocketFlow] = None,
      maybeGlobalMarketFilterRepository: Option[GlobalMarketFilterRepository] = None
  )(implicit clock: Clock, system: ActorSystem[_]): EitherT[Future, BetfairError, Betfair] = {
    implicit val ec = system.executionContext
    implicit val classicSystem = system.toClassic

    for {
      config <- loadConfig()
      authenticationService = createAuthenticationService(config)
      loginToBetfair = LoginToBetfair(authenticationService)
      sessionToken <- loginToBetfair().leftMap(FailedToLoginToBetfair(_)).leftUpcast[BetfairError]
    } yield {
      val (navigationService, exchangeService) = createUnderlyingServices(config, authenticationService)
      val applicationKey = config.auth.credentials.applicationKey
      val socketFlow = maybeSocketFlow.getOrElse(TlsSocketFlow.fromConfig(config.exchange.socket))
      val globalMarketFilterRepository = maybeGlobalMarketFilterRepository.getOrElse(InMemoryMarketFilterRepository())
      val marketDefinitionsRepository = new InMemoryMarketDefinitionsRepository()

      create(
        navigationService,
        exchangeService,
        applicationKey,
        sessionToken,
        globalMarketFilterRepository,
        marketDefinitionsRepository,
        socketFlow,
        config.exchange.socket
      )
    }
  }

//  def createClustered(
//      maybeSocketFlow: Option[TlsSocketFlow] = None,
//      maybeGlobalMarketFilterRepository: Option[GlobalMarketFilterRepository] = None
//  )(implicit clock: Clock, system: ActorSystem[_]): EitherT[Future, BetfairError, Betfair] = {
//    implicit val ec = system.executionContext
//    implicit val classicSystem = system.toClassic
//
//
//
//    loadConfig().flatMap { config =>
//      val underlyingAuthenticationService = createAuthenticationService(config)
//      val authenticationService = ClusterSingletonBetfairAuthenticationService(underlyingAuthenticationService)
//      authenticationService.login().map { sessionToken =>
//        val (underlyingNavigationService, underlyingExchangeService) =
//          createUnderlyingServices(config, authenticationService)
//        val navigationService = ClusterSingletonBetfairNavigationService(underlyingNavigationService)
//        val exchangeService = ClusterSingletonBetfairExchangeService(underlyingExchangeService)
//        val applicationKey = config.auth.credentials.applicationKey
//        val globalMarketFilterRepository = maybeGlobalMarketFilterRepository.getOrElse(InMemoryMarketFilterRepository())
//        val socketFlow = maybeSocketFlow.getOrElse(TlsSocketFlow.fromConfig(config.exchange.socket))
//
//        create(
//          navigationService,
//          exchangeService,
//          applicationKey,
//          sessionToken,
//          socketFlow,
//          globalMarketFilterRepository
//        )
//      }.leftMap(FailedToLoginToBetfair(_))
//    }
//  }

  private def create(
      navigationService: BetfairNavigationService,
      exchangeService: BetfairExchangeService,
      applicationKey: ApplicationKey,
      sessionToken: SessionToken,
      globalMarketFilterRepository: GlobalMarketFilterRepository,
      marketDefinitionsRepository: MarketDefinitionsRepository,
      socketFlow: TlsSocketFlow.TlsSocketFlow,
      socketConfig: SocketConfig
  )(implicit
      ec: ExecutionContext,
      system: ActorSystem[_]
  ): Betfair = {
    val getMenu = GetMenuUseCase(navigationService)

    val listAllEventTypes = ListAllEventTypesUseCase(exchangeService)
    val listEventTypes = ListEventTypesUseCase(exchangeService)
    val listEvents = ListEventsUseCase(exchangeService)

    val betfairSocket =
      BetfairSocket(
        socketFlow,
        applicationKey,
        sessionToken,
        globalMarketFilterRepository,
        marketDefinitionsRepository,
        socketConfig
      )

    Betfair(getMenu, listAllEventTypes, listEventTypes, listEvents, betfairSocket)
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

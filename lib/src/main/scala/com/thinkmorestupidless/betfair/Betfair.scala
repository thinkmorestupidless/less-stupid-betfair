package com.thinkmorestupidless.betfair

import cats.data.EitherT
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.LoginError
import com.thinkmorestupidless.betfair.auth.domain.{BetfairSession, SessionToken}
import com.thinkmorestupidless.betfair.auth.impl.PlayWsBetfairAuthenticationService
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService._
import com.thinkmorestupidless.betfair.exchange.domain._
import com.thinkmorestupidless.betfair.exchange.impl.AkkaHttpBetfairExchangeService
import org.apache.pekko.actor.ActorSystem
import org.slf4j.LoggerFactory
import pureconfig.error.ConfigReaderFailures

import scala.concurrent.{ExecutionContext, Future}

final class Betfair(val config: BetfairConfig, session: BetfairSession, exchange: BetfairExchangeService) {
  def cancelOrders(
      marketId: MarketId,
      instructions: List[CancelInstruction],
      customerRef: CustomerRef
  ): Future[CancelExecutionReport] =
    exchange.cancelOrders(marketId, instructions, customerRef)(session)

  def listClearedOrders(
      betStatus: BetStatus,
      eventTypeIds: Set[EventTypeId],
      eventIds: Set[EventId],
      marketIds: Set[MarketId],
      runnerIds: Set[RunnerId],
      betIds: Set[BetId],
      side: Side,
      customerOrderRefs: Set[CustomerOrderRef],
      customerStrategyRefs: Set[CustomerStrategyRef],
      settledDateRange: TimeRange,
      groupBy: GroupBy,
      includeItemDescription: Boolean,
      locale: String,
      fromRecord: Int,
      recordCount: Int
  ): Future[ClearedOrderSummaryReport] =
    exchange.listClearedOrders(
      betStatus,
      eventTypeIds,
      eventIds,
      marketIds,
      runnerIds,
      betIds,
      side,
      customerOrderRefs,
      customerStrategyRefs,
      settledDateRange,
      groupBy,
      includeItemDescription,
      locale,
      fromRecord,
      recordCount
    )(session)

  def listCompetitions(filter: MarketFilter): Future[List[CompetitionResult]] =
    exchange.listCompetitions(filter)(session)

  def listCountries(filter: MarketFilter): Future[List[CountryCodeResult]] =
    exchange.listCountries(filter)(session)

  def listCurrentOrders(
      betIds: Set[BetId],
      marketIds: Set[MarketId],
      orderProjection: OrderProjection,
      placedDateRange: TimeRange,
      dateRange: TimeRange,
      orderBy: OrderBy,
      sortDir: SortDir,
      fromRecord: Int,
      recordCount: Int
  ): Future[CurrentOrderSummaryReport] =
    exchange.listCurrentOrders(
      betIds,
      marketIds,
      orderProjection,
      placedDateRange,
      dateRange,
      orderBy,
      sortDir,
      fromRecord,
      recordCount
    )(session)

  def listEventTypes(filter: MarketFilter): Future[List[EventTypeResponse]] =
    exchange.listEventTypes(filter)(session)

  def listEvents(filter: MarketFilter): Future[Set[EventResponse]] =
    exchange.listEvents(filter)(session)

  def listMarketCatalogue(listMarketCatalogue: ListMarketCatalogue): Future[List[MarketCatalogue]] =
    exchange.listMarketCatalogue(listMarketCatalogue)(session)

  def listMarketBook(listMarketBook: ListMarketBook): Future[List[MarketBook]] =
    exchange.listMarketBook(listMarketBook)(session)

  def placeOrders(placeOrders: PlaceOrders): Future[PlaceExecutionReport] =
    exchange.placeOrders(placeOrders)(session)
}

object Betfair {

  private val log = LoggerFactory.getLogger(getClass)

  sealed trait BetfairError
  final case class FailedToLoadBetfairConfig(cause: ConfigReaderFailures) extends BetfairError
  final case class FailedBetfairAuthentication(cause: LoginError) extends BetfairError

  def apply(system: ActorSystem): EitherT[Future, BetfairError, Betfair] = {
    implicit val sys = system
    implicit val ec = system.dispatcher

    this()
  }

//  def apply(system: typed.ActorSystem[_]): EitherT[Future, BetfairError, Betfair] = {
//    implicit val sys = system.classicSystem
//    implicit val ec = system.executionContext
//
//    this()
//  }

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
    } yield (config, BetfairSession(config.login.credentials.applicationKey, sessionToken))

  private def authenticate(
      config: BetfairConfig
  )(implicit system: ActorSystem, ec: ExecutionContext): EitherT[Future, BetfairError, SessionToken] = {
    log.info("authenticating")
    val authenticator = new PlayWsBetfairAuthenticationService(config)
    log.info(s"authenticator: $authenticator")
    authenticator.login().leftMap(FailedBetfairAuthentication(_))
  }

  private def loadConfig()(implicit ec: ExecutionContext): EitherT[Future, BetfairError, BetfairConfig] =
    EitherT.fromEither[Future](BetfairConfig.load()).leftMap(FailedToLoadBetfairConfig(_))
}

package com.thinkmorestupidless.betfair.exchange.impl

import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.ListEventTypes
import com.thinkmorestupidless.betfair.exchange.domain.{BetId, BetStatus, BetfairExchangeService, CancelExecutionReport, CancelInstruction, ClearedOrderSummaryReport, CompetitionResult, CountryCodeResult, CurrentOrderSummaryReport, CustomerOrderRef, CustomerRef, CustomerStrategyRef, EventId, EventTypeId, GroupBy, MarketBook, MarketCatalogue, MarketFilter, MarketId, OrderBy, OrderProjection, PlaceExecutionReport, PlaceOrders, RunnerId, Side, SortDir, TimeRange}
import org.apache.pekko.actor.ActorSystem
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import com.thinkmorestupidless.betfair.exchange.impl.BodyWritables._

import scala.concurrent.Future

class PlayWsBetfairExchangeService(config: BetfairConfig)(implicit system: ActorSystem) extends BetfairExchangeService {

  private implicit val ec = system.dispatcher

  private val wsClient = StandaloneAhcWSClient()

  override def cancelOrders(marketId: MarketId, instructions: List[CancelInstruction], customerRef: CustomerRef)(implicit session: BetfairSession): Future[CancelExecutionReport] = ???

  override def listClearedOrders(betStatus: BetStatus, eventTypeIds: Set[EventTypeId], eventIds: Set[EventId], marketIds: Set[MarketId], runnerIds: Set[RunnerId], betIds: Set[BetId], side: Side, customerOrderRefs: Set[CustomerOrderRef], customerStrategyRefs: Set[CustomerStrategyRef], settledDateRange: TimeRange, groupBy: GroupBy, includeItemDescription: Boolean, locale: String, fromRecord: Int, recordCount: Int)(implicit session: BetfairSession): Future[ClearedOrderSummaryReport] = ???

  override def listCompetitions(filter: MarketFilter)(implicit session: BetfairSession): Future[List[CompetitionResult]] = ???

  override def listCountries(filter: MarketFilter)(implicit session: BetfairSession): Future[List[CountryCodeResult]] = ???

  override def listCurrentOrders(betIds: Set[BetId], marketIds: Set[MarketId], orderProjection: OrderProjection, placedDateRange: TimeRange, dateRange: TimeRange, orderBy: OrderBy, sortDir: SortDir, fromRecord: Int, recordCount: Int)(implicit session: BetfairSession): Future[CurrentOrderSummaryReport] = ???

  override def listEventTypes(filter: MarketFilter)(implicit session: BetfairSession): Future[List[BetfairExchangeService.EventTypeResponse]] = {
    wsClient.url(config.exchange.uris.listEventTypes.value)
      .addHttpHeaders((config.headerKeys.applicationKey.value, session.applicationKey.value), (config.headerKeys.sessionToken.value, session.sessionToken.value))
      .post(ListEventTypes(filter))
      .map { response =>
        ???
      }
  }

  override def listEvents(filter: MarketFilter)(implicit session: BetfairSession): Future[Set[BetfairExchangeService.EventResponse]] = ???

  override def listMarketCatalogue(listMarketCatalogue: BetfairExchangeService.ListMarketCatalogue)(implicit session: BetfairSession): Future[List[MarketCatalogue]] = ???

  override def listMarketBook(listMarketBook: BetfairExchangeService.ListMarketBook)(implicit session: BetfairSession): Future[List[MarketBook]] = ???

  override def placeOrders(placeOrders: PlaceOrders)(implicit session: BetfairSession): Future[PlaceExecutionReport] = ???
}

package com.thinkmorestupidless.betfair.exchange.impl.grpc

import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService
import com.thinkmorestupidless.betfair.proto.exchange.{
  CancelExecutionReport,
  CancelOrdersRequest,
  ClearedOrderSummaryReport,
  CompetitionResult,
  CountryCodeResponse,
  CurrentOrderSummaryReport,
  ExchangeService,
  ListCurrentOrdersRequest,
  ListEventTypesResponse,
  ListEventsResponse,
  ListMarketBookRequest,
  ListMarketBookResponse,
  ListMarketCatalogueRequest,
  ListMarketCatalogueResponse,
  MarketFilter,
  PlaceExecutionReport,
  PlaceOrdersRequest
}

import scala.concurrent.Future

class GprcExchangeService(betfairExchangeService: BetfairExchangeService) extends ExchangeService {

  override def cancelOrders(in: CancelOrdersRequest): Future[CancelExecutionReport] = ???

  override def listClearedOrders(in: MarketFilter): Future[ClearedOrderSummaryReport] = ???

  override def listCompetitions(in: MarketFilter): Future[CompetitionResult] = ???

  override def listCountries(in: MarketFilter): Future[CountryCodeResponse] = ???

  override def listCurrentOrders(in: ListCurrentOrdersRequest): Future[CurrentOrderSummaryReport] = ???

  override def listEventTypes(in: MarketFilter): Future[ListEventTypesResponse] = ???

  override def listEvents(in: MarketFilter): Future[ListEventsResponse] = ???

  override def listMarketCatalogue(in: ListMarketCatalogueRequest): Future[ListMarketCatalogueResponse] = ???

  override def listMarketBook(in: ListMarketBookRequest): Future[ListMarketBookResponse] = ???

  override def placeOrders(in: PlaceOrdersRequest): Future[PlaceExecutionReport] = ???
}

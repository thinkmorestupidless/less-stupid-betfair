package com.thinkmorestupidless.betfair.exchange.impl.grpc

import com.thinkmorestupidless.grpc.Decoder._
import com.thinkmorestupidless.grpc.Encoder._
import com.thinkmorestupidless.betfair.exchange.impl.grpc.Decoders._
import com.thinkmorestupidless.betfair.exchange.impl.grpc.Encoders._
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.{EventTypeResponse, ListEventsResponse}
import com.thinkmorestupidless.betfair.exchange.impl.grpc.GprcExchangeService.ListEventTypesResponse
import com.thinkmorestupidless.betfair.exchange.usecases.ListEventTypesUseCase.ListEventTypesUseCase
import com.thinkmorestupidless.betfair.exchange.usecases.ListEventsUseCase.ListEventsUseCase
import com.thinkmorestupidless.betfair.proto.exchange.{
  CancelExecutionReport,
  CancelOrdersRequest,
  ClearedOrderSummaryReport,
  CompetitionResult,
  CountryCodeResponse,
  CurrentOrderSummaryReport,
  ExchangeService,
  ListCurrentOrdersRequest,
  ListEventTypesResponse => ListEventTypesResponseProto,
  ListEventsResponse => ListEventsResponseProto,
  ListMarketBookRequest,
  ListMarketBookResponse,
  ListMarketCatalogueRequest,
  ListMarketCatalogueResponse,
  MarketFilter,
  PlaceExecutionReport,
  PlaceOrdersRequest
}
import com.thinkmorestupidless.utils.ValidationException

import scala.concurrent.{ExecutionContext, Future}

final class GprcExchangeService(listEventTypesUseCase: ListEventTypesUseCase, listEventsUseCase: ListEventsUseCase)(
    implicit ec: ExecutionContext
) extends ExchangeService {

  override def cancelOrders(in: CancelOrdersRequest): Future[CancelExecutionReport] = ???

  override def listClearedOrders(in: MarketFilter): Future[ClearedOrderSummaryReport] = ???

  override def listCompetitions(in: MarketFilter): Future[CompetitionResult] = ???

  override def listCountries(in: MarketFilter): Future[CountryCodeResponse] = ???

  override def listCurrentOrders(in: ListCurrentOrdersRequest): Future[CurrentOrderSummaryReport] = ???

  override def listEventTypes(in: MarketFilter): Future[ListEventTypesResponseProto] =
    in.decode.fold(
      errors => Future.failed(ValidationException.combineErrors(errors)),
      decoded =>
        listEventTypesUseCase(decoded).value.flatMap {
          case Right(result) => Future.successful(ListEventTypesResponse(result).encode)
          case Left(error)   => Future.failed(error.toValidationException())
        }
    )

  override def listEvents(in: MarketFilter): Future[ListEventsResponseProto] =
    in.decode.fold(
      errors => Future.failed(ValidationException.combineErrors(errors)),
      decoded =>
        listEventsUseCase(decoded).value.flatMap {
          case Right(result) => Future.successful(ListEventsResponse(result).encode)
          case Left(error)   => Future.failed(error.toValidationException())
        }
    )

  override def listMarketCatalogue(in: ListMarketCatalogueRequest): Future[ListMarketCatalogueResponse] = ???

  override def listMarketBook(in: ListMarketBookRequest): Future[ListMarketBookResponse] = ???

  override def placeOrders(in: PlaceOrdersRequest): Future[PlaceExecutionReport] = ???
}

object GprcExchangeService {

  final case class ListEventTypesResponse(results: List[EventTypeResponse])
}

package com.thinkmorestupidless.betfair.exchange.domain

import cats.data.EitherT
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.AuthenticationError
import com.thinkmorestupidless.betfair.auth.domain.SessionToken
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.{
  EventResponse,
  EventTypeResponse,
  ExchangeServiceError,
  ListMarketBook,
  ListMarketCatalogue
}

import java.time.Instant
import scala.concurrent.Future

trait BetfairExchangeService {

  def cancelOrders(
      marketId: MarketId,
      instructions: List[CancelInstruction],
      customerRef: CustomerRef
  ): EitherT[Future, ExchangeServiceError, CancelExecutionReport]

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
  ): EitherT[Future, ExchangeServiceError, ClearedOrderSummaryReport]

  def listCompetitions(filter: MarketFilter): EitherT[Future, ExchangeServiceError, List[CompetitionResult]]

  def listCountries(filter: MarketFilter): EitherT[Future, ExchangeServiceError, List[CountryCodeResult]]

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
  ): EitherT[Future, ExchangeServiceError, CurrentOrderSummaryReport]

  def listEventTypes(filter: MarketFilter): EitherT[Future, ExchangeServiceError, List[EventTypeResponse]]

  def listEvents(filter: MarketFilter): EitherT[Future, ExchangeServiceError, Set[EventResponse]]

  def listMarketCatalogue(
      listMarketCatalogue: ListMarketCatalogue
  ): EitherT[Future, ExchangeServiceError, List[MarketCatalogue]]

  def listMarketBook(listMarketBook: ListMarketBook): EitherT[Future, ExchangeServiceError, List[MarketBook]]

  def placeOrders(placeOrders: PlaceOrders): EitherT[Future, ExchangeServiceError, PlaceExecutionReport]
}

object BetfairExchangeService {

  sealed trait ExchangeServiceError
  final case class FailedAuthentication(error: AuthenticationError) extends ExchangeServiceError
  final case class UnableToExecuteRequest(cause: Throwable) extends ExchangeServiceError
  final case class UnableToHandleResponse(cause: Throwable) extends ExchangeServiceError

  case class CancelOrders(marketId: MarketId, instructions: List[CancelInstruction], customerRef: CustomerRef)
  case class ListClearedOrders(
      betStatus: BetStatus,
      eventTypeIds: Set[EventTypeId],
      eventIds: Set[EventId],
      marketIds: Set[MarketId],
      runnerIds: Set[RunnerId],
      betIds: Set[BetId],
      customerOrderRefs: Set[CustomerOrderRef],
      customerStrategyRefs: Set[CustomerStrategyRef],
      side: Side,
      settledDateRange: TimeRange,
      groupBy: GroupBy,
      includeItemDescription: Boolean,
      locale: String,
      fromRecord: Int,
      recordCount: Int
  )
  case class ListCompetitions(filter: MarketFilter, locale: Option[String])
  case class ListCountries(filter: MarketFilter, locale: Option[String])
  case class ListCurrentOrders(
      betIds: Set[BetId],
      marketIds: Set[MarketId],
      orderProjection: OrderProjection,
      placedDateRange: TimeRange,
      dateRange: TimeRange,
      orderBy: OrderBy,
      sortDir: SortDir,
      fromRecord: Int,
      recordCount: Int
  )

  case class KeepAliveResponse(token: String, product: String, status: String, error: Option[String])

  case class EventTypeResponse(eventType: EventType, marketCount: Int)
  case class EventResponse(event: Event, marketCount: Int)
  case class ListRaceDetailsResponse(jsonrpc: String, id: Int, result: List[_])

  case class ListEventTypes(filter: MarketFilter, locale: Option[String] = None)
  case class ListEvents(filter: MarketFilter, locale: Option[String] = None)
  case class ListEventsResponse(results: List[EventResponse])

  case class ListMarketBook(
      marketIds: Option[List[MarketId]],
      priceProjection: Option[PriceProjection],
      orderProjection: Option[OrderProjection],
      matchProjection: Option[MatchProjection],
      includeOverallPosition: Option[IncludeOverallPosition],
      partitionMatchedByStrategyRef: Option[PartitionMatchedByStrategyRef],
      customerStrategyRefs: Option[Set[CustomerStrategyRef]],
      currencyCode: Option[CurrencyCode],
      locale: Option[Locale],
      matchedSince: Option[MatchedSince],
      betIds: Option[Set[BetId]]
  )

  case class MatchedSince(value: Instant)

  case class MaxResults(value: Int)
  object MaxResults {
    def default = MaxResults(100)
  }

  case class Locale(value: String)
  object Locale {
    val EN = Locale("en")
  }

  case class ListMarketCatalogue(
      filter: MarketFilter,
      maxResults: MaxResults,
      marketProjection: Option[Set[MarketProjection]],
      sort: Option[MarketSort],
      locale: Option[Locale]
  )
  object ListMarketCatalogue {

    def apply(
        filter: MarketFilter,
        maxResults: MaxResults = MaxResults.default,
        marketProjection: Option[Set[MarketProjection]] = None,
        sort: Option[MarketSort] = None,
        locale: Option[Locale] = None
    ) =
      new ListMarketCatalogue(filter, maxResults, marketProjection, sort, locale)
  }

  case class ListRaceDetails(raceIds: List[String], meetingIds: List[String])
}

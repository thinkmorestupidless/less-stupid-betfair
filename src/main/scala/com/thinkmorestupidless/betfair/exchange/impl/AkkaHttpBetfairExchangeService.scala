package com.thinkmorestupidless.betfair.exchange.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.coding.Coders
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.{HttpEncodings, RawHeader}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService._
import com.thinkmorestupidless.betfair.exchange.domain._
import com.thinkmorestupidless.betfair.exchange.impl.JsonCodecs._
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import io.circe.Decoder
import io.circe.parser.decode

import scala.concurrent.Future

final class AkkaHttpBetfairExchangeService(config: BetfairConfig)(implicit system: ActorSystem)
    extends BetfairExchangeService {

  implicit private val ec = system.dispatcher

  override def cancelOrders(marketId: MarketId, instructions: List[CancelInstruction], customerRef: CustomerRef)(
      implicit session: BetfairSession
  ): Future[CancelExecutionReport] =
    execute[CancelOrders, CancelExecutionReport](
      CancelOrders(marketId, instructions, customerRef),
      config.exchange.uris.cancelOrders.value
    )

  override def listClearedOrders(
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
  )(implicit session: BetfairSession): Future[ClearedOrderSummaryReport] =
    execute[ListClearedOrders, ClearedOrderSummaryReport](
      ListClearedOrders(
        betStatus,
        eventTypeIds,
        eventIds,
        marketIds,
        runnerIds,
        betIds,
        customerOrderRefs,
        customerStrategyRefs,
        side,
        settledDateRange,
        groupBy,
        includeItemDescription,
        locale,
        fromRecord,
        recordCount
      ),
      config.exchange.uris.listClearedOrders.value
    )

  override def listCompetitions(
      filter: MarketFilter
  )(implicit session: BetfairSession): Future[List[CompetitionResult]] =
    execute[ListCompetitions, List[CompetitionResult]](
      ListCompetitions(filter, locale = None),
      config.exchange.uris.listCompetitions.value
    )

  override def listCountries(filter: MarketFilter)(implicit session: BetfairSession): Future[List[CountryCodeResult]] =
    execute[ListCountries, List[CountryCodeResult]](
      ListCountries(filter, locale = None),
      config.exchange.uris.listCountries.value
    )

  override def listCurrentOrders(
      betIds: Set[BetId],
      marketIds: Set[MarketId],
      orderProjection: OrderProjection,
      placedDateRange: TimeRange,
      dateRange: TimeRange,
      orderBy: OrderBy,
      sortDir: SortDir,
      fromRecord: Int,
      recordCount: Int
  )(implicit session: BetfairSession): Future[CurrentOrderSummaryReport] =
    execute[ListCurrentOrders, CurrentOrderSummaryReport](
      ListCurrentOrders(
        betIds,
        marketIds,
        orderProjection,
        placedDateRange,
        dateRange,
        orderBy,
        sortDir,
        fromRecord,
        recordCount
      ),
      config.exchange.uris.listCurrentOrders.value
    )

  override def listEventTypes(
      filter: MarketFilter
  )(implicit session: BetfairSession): Future[List[EventTypeResponse]] =
    execute[ListEventTypes, List[EventTypeResponse]](ListEventTypes(filter), config.exchange.uris.listEventTypes.value)

  override def listEvents(filter: MarketFilter)(implicit session: BetfairSession): Future[Set[EventResponse]] =
    execute[ListEvents, Set[EventResponse]](ListEvents(filter), config.exchange.uris.listEvents.value)

  override def listMarketCatalogue(
      listMarketCatalogue: ListMarketCatalogue
  )(implicit session: BetfairSession): Future[List[MarketCatalogue]] =
    execute[ListMarketCatalogue, List[MarketCatalogue]](
      listMarketCatalogue,
      config.exchange.uris.listMarketCatalogue.value
    )

  override def listMarketBook(listMarketBook: ListMarketBook)(implicit
      session: BetfairSession
  ): Future[List[MarketBook]] =
    execute[ListMarketBook, List[MarketBook]](listMarketBook, config.exchange.uris.listMarketBook.value)

  override def placeOrders(placeOrders: PlaceOrders)(implicit session: BetfairSession): Future[PlaceExecutionReport] =
    execute[PlaceOrders, PlaceExecutionReport](placeOrders, config.exchange.uris.placeOrders.value)

  private def execute[REQUEST, RESPONSE](content: REQUEST, uri: String)(implicit
      decoder: Decoder[RESPONSE],
      m: ToEntityMarshaller[REQUEST],
      session: BetfairSession
  ): Future[RESPONSE] = {
    val headers = config.exchange.requiredHeaders ++ List(
      RawHeader(config.headerKeys.applicationKey.value, session.applicationKey.value),
      RawHeader(config.headerKeys.sessionToken.value, session.sessionToken.value)
    )
    val request = Post(uri = uri, content = content).withHeaders(headers)
    for {
      response <- Http().singleRequest(request).map(decodeResponse)
      resultString <- Unmarshal(response.entity).to[String]
      _ = println(resultString)
      result = decode[RESPONSE](resultString).getOrElse(throw new IllegalStateException("failed thingy"))
    } yield result
  }

  private def decodeResponse(response: HttpResponse): HttpResponse = {
    val decoder = response.encoding match {
      case HttpEncodings.gzip =>
        Coders.Gzip
      case HttpEncodings.deflate =>
        Coders.Deflate
      case HttpEncodings.identity =>
        Coders.NoCoding
      case other =>
        system.log.warning(s"Unknown encoding [$other], not decoding")
        Coders.NoCoding
    }

    decoder.decodeMessage(response)
  }
}

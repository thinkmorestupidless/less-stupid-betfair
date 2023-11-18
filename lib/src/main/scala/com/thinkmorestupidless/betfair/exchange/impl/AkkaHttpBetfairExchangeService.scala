package com.thinkmorestupidless.betfair.exchange.impl

import cats.data.EitherT
import com.thinkmorestupidless.betfair.auth.domain.{BetfairAuthenticationService, SessionToken}
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService._
import com.thinkmorestupidless.betfair.exchange.domain._
import com.thinkmorestupidless.betfair.exchange.impl.JsonCodecs._
import com.thinkmorestupidless.utils.CirceSupport
import io.circe.Decoder
import io.circe.parser.decode
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.client.RequestBuilding
import org.apache.pekko.http.scaladsl.coding.Coders
import org.apache.pekko.http.scaladsl.marshalling.ToEntityMarshaller
import org.apache.pekko.http.scaladsl.model.headers.{HttpEncodings, RawHeader}
import org.apache.pekko.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

final class AkkaHttpBetfairExchangeService(config: BetfairConfig, authenticationService: BetfairAuthenticationService)(
    implicit
    system: ActorSystem,
    ec: ExecutionContext
) extends BetfairExchangeService
    with CirceSupport {

  private val log = LoggerFactory.getLogger(getClass)

  private def withSessionToken[T](
      f: SessionToken => EitherT[Future, ExchangeServiceError, T]
  ): EitherT[Future, ExchangeServiceError, T] =
    authenticationService.login().leftMap(FailedAuthentication(_)).flatMap(f(_))

  override def cancelOrders(
      marketId: MarketId,
      instructions: List[CancelInstruction],
      customerRef: CustomerRef
  ): EitherT[Future, ExchangeServiceError, CancelExecutionReport] =
    withSessionToken(
      execute[CancelOrders, CancelExecutionReport](
        _,
        CancelOrders(marketId, instructions, customerRef),
        config.exchange.uris.cancelOrders.value
      )
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
  ): EitherT[Future, ExchangeServiceError, ClearedOrderSummaryReport] =
    withSessionToken(
      execute[ListClearedOrders, ClearedOrderSummaryReport](
        _,
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
    )

  override def listCompetitions(filter: MarketFilter): EitherT[Future, ExchangeServiceError, List[CompetitionResult]] =
    withSessionToken(
      execute[ListCompetitions, List[CompetitionResult]](
        _,
        ListCompetitions(filter, locale = None),
        config.exchange.uris.listCompetitions.value
      )
    )

  override def listCountries(filter: MarketFilter): EitherT[Future, ExchangeServiceError, List[CountryCodeResult]] =
    withSessionToken(
      execute[ListCountries, List[CountryCodeResult]](
        _,
        ListCountries(filter, locale = None),
        config.exchange.uris.listCountries.value
      )
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
  ): EitherT[Future, ExchangeServiceError, CurrentOrderSummaryReport] =
    withSessionToken(
      execute[ListCurrentOrders, CurrentOrderSummaryReport](
        _,
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
    )

  override def listEventTypes(filter: MarketFilter): EitherT[Future, ExchangeServiceError, List[EventTypeResponse]] =
    withSessionToken(
      execute[ListEventTypes, List[EventTypeResponse]](
        _,
        ListEventTypes(filter),
        config.exchange.uris.listEventTypes.value
      )
    )

  override def listEvents(filter: MarketFilter): EitherT[Future, ExchangeServiceError, List[EventResponse]] =
    withSessionToken(
      execute[ListEvents, List[EventResponse]](_, ListEvents(filter), config.exchange.uris.listEvents.value)
    )

  override def listMarketCatalogue(
      listMarketCatalogue: ListMarketCatalogue
  ): EitherT[Future, ExchangeServiceError, List[MarketCatalogue]] =
    withSessionToken(
      execute[ListMarketCatalogue, List[MarketCatalogue]](
        _,
        listMarketCatalogue,
        config.exchange.uris.listMarketCatalogue.value
      )
    )

  override def listMarketBook(listMarketBook: ListMarketBook): EitherT[Future, ExchangeServiceError, List[MarketBook]] =
    withSessionToken(
      execute[ListMarketBook, List[MarketBook]](_, listMarketBook, config.exchange.uris.listMarketBook.value)
    )

  override def placeOrders(placeOrders: PlaceOrders): EitherT[Future, ExchangeServiceError, PlaceExecutionReport] =
    withSessionToken(execute[PlaceOrders, PlaceExecutionReport](_, placeOrders, config.exchange.uris.placeOrders.value))

  private def execute[REQUEST, RESPONSE](sessionToken: SessionToken, requestBody: REQUEST, uri: String)(implicit
      decoder: Decoder[RESPONSE],
      m: ToEntityMarshaller[REQUEST]
  ): EitherT[Future, ExchangeServiceError, RESPONSE] = {
    val headers: Seq[HttpHeader] = config.exchange.requiredHeaders ++ List(
      RawHeader(config.headerKeys.applicationKey.value, config.auth.credentials.applicationKey.value),
      RawHeader(config.headerKeys.sessionToken.value, sessionToken.value)
    )
    val httpRequest = RequestBuilding.Post(uri = uri, content = requestBody).withHeaders(headers)
    for {
      httpResponse <- send(httpRequest)
      responseBodyAsString <- unmarshalToString(httpResponse)
      _ = if (config.exchange.logging.logResponses.value) log.info(responseBodyAsString)
      result <- decodeAs[RESPONSE](responseBodyAsString)
    } yield result
  }

  private def send(request: HttpRequest): EitherT[Future, ExchangeServiceError, HttpResponse] =
    EitherT.liftF(Http().singleRequest(request).map(decodeResponse)).leftMap(UnableToExecuteRequest(_))

  private def unmarshalToString(response: HttpResponse): EitherT[Future, ExchangeServiceError, String] =
    EitherT.liftF(Unmarshal(response.entity).to[String]).leftMap(UnableToHandleResponse(_))

  private def decodeAs[T: Decoder](resultString: String)(implicit
      ec: ExecutionContext
  ): EitherT[Future, ExchangeServiceError, T] =
    EitherT.fromEither[Future](decode[T](resultString)).leftMap(UnableToHandleResponse(_))

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

package com.thinkmorestupidless.betfair.exchange.impl.grpc

import cats.syntax.apply._
import cats.syntax.traverse._
import cats.syntax.validated._
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.EventTypeResponse
import com.thinkmorestupidless.betfair.exchange.domain.{
  Competition,
  CompetitionId,
  CompetitionName,
  CountryCode,
  Event,
  EventId,
  EventName,
  EventType,
  EventTypeId,
  EventTypeName,
  MarketCatalogue,
  MarketDescription,
  MarketFilter,
  MarketId,
  MarketName,
  MarketStartTime,
  OpenDate,
  RunnerCatalog,
  RunnerMetadata,
  Timezone,
  TotalMatched,
  Venue
}
import com.thinkmorestupidless.betfair.exchange.impl.grpc.GprcExchangeService.ListEventTypesResponse
import com.thinkmorestupidless.betfair.proto.exchange.{
  Competition => CompetitionProto,
  Event => EventProto,
  EventType => EventTypeProto,
  EventTypeResponse => EventTypeResponseProto,
  ListEventTypesResponse => ListEventTypesResponseProto,
  MarketCatalogue => MarketCatalogueProto,
  MarketDescription => MarketDescriptionProto,
  MarketFilter => MarketFilterProto,
  RunnerCatalog => RunnerCatalogProto
}
import com.thinkmorestupidless.grpc.Decoder
import com.thinkmorestupidless.grpc.Decoder._
import com.thinkmorestupidless.grpc.DefaultDecoders._
import com.thinkmorestupidless.utils.Validation.Validation
import com.thinkmorestupidless.utils.Validation.ImplicitConversions.toValidatedOptionalList

import java.time.Instant

object Decoders {

  implicit val marketFilterProto_marketFilter: Decoder[MarketFilterProto, MarketFilter] =
    proto => ???

  implicit val listEventsResponseProto_listEventsResponse
      : Decoder[ListEventTypesResponseProto, ListEventTypesResponse] =
    proto => proto.results.toList.map(_.decode).sequence.map(ListEventTypesResponse(_))

  implicit val eventTypeResponseProto_eventTypeResponse: Decoder[EventTypeResponseProto, EventTypeResponse] =
    proto =>
      ensureOptionIsDefined(proto.eventType, "required field EventTypeResponse.eventType is not set")
        .andThen(_.decode)
        .map(EventTypeResponse(_, proto.marketCount))

  implicit val eventTypeProto_eventType: Decoder[EventTypeProto, EventType] =
    proto => {
      val id = proto.id.decode[EventTypeId]
      val name = proto.name.decode[EventTypeName]

      (id, name).mapN(EventType.apply _)
    }

  implicit val string_eventTypeId: Decoder[String, EventTypeId] = EventTypeId(_).validNel
  implicit val string_eventTypeName: Decoder[String, EventTypeName] = EventTypeName(_).validNel

  implicit val eventProto_event: Decoder[EventProto, Event] =
    proto => {
      val id = proto.id.decode[EventId]
      val name = proto.name.decode[EventName]
      val countryCode = proto.countryCode.decode[Option[CountryCode]]
      val timezone = proto.timezone.decode[Timezone]
      val venue = proto.venue.decode[Option[Venue]]
      val openDate = proto.openDate.decode[OpenDate]

      (id, name, countryCode, timezone, venue, openDate).mapN(Event.apply _)
    }

  implicit val string_eventId: Decoder[String, EventId] = EventId(_).validNel
  implicit val string_eventName: Decoder[String, EventName] = EventName(_).validNel
  implicit val string_timezone: Decoder[String, Timezone] = Timezone(_).validNel
  implicit val optionalString_optionalVenue: Decoder[Option[String], Option[Venue]] = _.map(Venue(_)).validNel
  implicit val string_openDate: Decoder[String, OpenDate] = str => OpenDate(Instant.parse(str)).validNel
  implicit val optionalString_optionalCountryCode: Decoder[Option[String], Option[CountryCode]] =
    _.map(CountryCode(_)).validNel

  implicit val marketCatalogueProto_marketCatalogue: Decoder[MarketCatalogueProto, MarketCatalogue] =
    proto => {
      val marketId = proto.marketId.decode[MarketId]
      val marketName = proto.marketName.decode[MarketName]
      val marketStartTime = proto.marketStartTime.decode[Option[MarketStartTime]]
      val description = proto.description.decode[Option[MarketDescription]]
      val runners: Validation[Option[List[RunnerCatalog]]] = proto.runners.toList.map(_.decode).sequence
      val eventType = proto.eventType.decode[Option[EventType]]
      val competition = proto.competition.decode[Option[Competition]]
      val event = proto.event.decode[Option[Event]]
      val totalMatched = proto.totalMatched.decode[TotalMatched]

      (marketId, marketName, marketStartTime, description, runners, eventType, competition, event, totalMatched).mapN(
        MarketCatalogue.apply _
      )
    }

  implicit val marketDescriptionProto_marketDescription: Decoder[MarketDescriptionProto, MarketDescription] = ???

  implicit val runnerCatalogProto_runnerCatalog: Decoder[RunnerCatalogProto, RunnerCatalog] =
    proto => {
      val sortPriority = proto.sortPriority.decode[Int]
      val selectionId = proto.selectionId.decode[Long]
      val runnerName = proto.runnerName.decode[String]
      val handicap = proto.handicap.decode[BigDecimal]
      val metadata = validNone[RunnerMetadata]

      (sortPriority, selectionId, runnerName, handicap, metadata).mapN(RunnerCatalog.apply _)
    }

  implicit val competitionProto_competition: Decoder[CompetitionProto, Competition] =
    proto => {
      val id = proto.id.decode[CompetitionId]
      val name = proto.name.decode[CompetitionName]

      (id, name).mapN(Competition.apply _)
    }

  implicit val string_competitionId: Decoder[String, CompetitionId] = CompetitionId(_).validNel
  implicit val string_competitionName: Decoder[String, CompetitionName] = CompetitionName(_).validNel
  implicit val string_marketId: Decoder[String, MarketId] = MarketId(_).validNel
  implicit val string_marketName: Decoder[String, MarketName] = MarketName(_).validNel
  implicit val string_totalMatched: Decoder[String, TotalMatched] = str => TotalMatched(BigDecimal(str)).validNel
  implicit val optionalString_optionalMarketStartTime: Decoder[Option[String], Option[MarketStartTime]] =
    _.map(str => MarketStartTime(Instant.parse(str))).validNel
}

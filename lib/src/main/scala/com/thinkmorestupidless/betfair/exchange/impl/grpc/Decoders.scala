package com.thinkmorestupidless.betfair.exchange.impl.grpc

import cats.syntax.apply._
import cats.syntax.traverse._
import cats.syntax.validated._
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.EventTypeResponse
import com.thinkmorestupidless.betfair.exchange.domain.{
  EventId,
  EventName,
  EventType,
  EventTypeId,
  EventTypeName,
  MarketFilter
}
import com.thinkmorestupidless.betfair.exchange.impl.grpc.GprcExchangeService.ListEventTypesResponse
import com.thinkmorestupidless.betfair.proto.exchange.{
  EventType => EventTypeProto,
  EventTypeResponse => EventTypeResponseProto,
  ListEventTypesResponse => ListEventTypesResponseProto,
  MarketFilter => MarketFilterProto
}
import com.thinkmorestupidless.grpc.Decoder
import com.thinkmorestupidless.grpc.Decoder._

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

  implicit val string_eventId: Decoder[String, EventTypeId] = EventTypeId(_).validNel
  implicit val string_eventName: Decoder[String, EventTypeName] = EventTypeName(_).validNel
}

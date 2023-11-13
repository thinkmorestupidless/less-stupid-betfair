package com.thinkmorestupidless.betfair.exchange.impl.grpc

import cats.syntax.option._
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.{
  EventResponse,
  EventTypeResponse,
  ListEventsResponse
}
import com.thinkmorestupidless.betfair.exchange.domain.{Event, EventType}
import com.thinkmorestupidless.betfair.exchange.impl.grpc.GprcExchangeService.ListEventTypesResponse
import com.thinkmorestupidless.betfair.proto.exchange.{
  Event => EventProto,
  EventResponse => EventResponseProto,
  EventType => EventTypeProto,
  EventTypeResponse => EventTypeResponseProto,
  ListEventTypesResponse => ListEventTypesResponseProto,
  ListEventsResponse => ListEventsResponseProto
}
import com.thinkmorestupidless.grpc.Encoder
import com.thinkmorestupidless.grpc.Encoder._

object Encoders {

  implicit val listEventTypesResponse_listEventTypesResponseProto
      : Encoder[ListEventTypesResponse, ListEventTypesResponseProto] =
    listEventTypesResponse =>
      ListEventTypesResponseProto.defaultInstance.withResults(
        listEventTypesResponse.results.toSeq.map(_.encode)
      )

  implicit val eventTypeResponse_eventTypeResponseProto: Encoder[EventTypeResponse, EventTypeResponseProto] =
    eventTypeResponse =>
      EventTypeResponseProto.defaultInstance
        .withEventType(eventTypeResponse.eventType.encode)
        .withMarketCount(eventTypeResponse.marketCount)

  implicit val eventType_eventTypeProto: Encoder[EventType, EventTypeProto] =
    eventType => EventTypeProto.defaultInstance.withId(eventType.id.value).withName(eventType.name.value)

  implicit val listEventsResponse_listEventsResponseProto: Encoder[ListEventsResponse, ListEventsResponseProto] =
    listEventsResponse => ListEventsResponseProto.defaultInstance.withResults(listEventsResponse.results.map(_.encode))

  implicit val eventResponse_eventResponseProto: Encoder[EventResponse, EventResponseProto] =
    eventResponse => EventResponseProto(eventResponse.event.encode.some, eventResponse.marketCount)

  implicit val event_eventProto: Encoder[Event, EventProto] =
    event =>
      EventProto(
        event.id.value,
        event.name.value,
        event.countryCode.map(_.value),
        event.timezone.value,
        event.venue.map(_.value),
        event.openDate.value.toString
      )
}

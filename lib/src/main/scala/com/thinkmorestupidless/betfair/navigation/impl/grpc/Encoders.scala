package com.thinkmorestupidless.betfair.navigation.impl.grpc

import com.thinkmorestupidless.betfair.navigation.domain._
import com.thinkmorestupidless.betfair.proto.navigation.{
  Event => EventProto,
  EventType => EventTypeProto,
  Group => GroupProto,
  Market => MarketProto,
  Menu => MenuProto,
  Race => RaceProto
}
import com.thinkmorestupidless.grpc.Encoder

object Encoders {

  implicit val menuEncoder: Encoder[Menu, MenuProto] =
    menu => {
      val children = menu.children.map { child =>
        child match {
          case eventType: EventType => Some(encodeEventType(eventType))
          case _                    => None
        }
      }.flatten
      MenuProto().withEventTypes(children)
    }

  private def encodeEventType(eventType: EventType): EventTypeProto =
    EventTypeProto.defaultInstance
      .withId(eventType.id.value)
      .withName(eventType.name.value)
      .withEvents(eventType.events.map(encodeEvent))
      .withGroups(eventType.groups.map(encodeGroup))
      .withRaces(eventType.races.map(encodeRace))

  private def encodeEvent(event: Event): EventProto =
    EventProto.defaultInstance
      .withId(event.id.value)
      .withName(event.name.value)
      .withCountryCode(event.countryCode.value)
      .withMarkets(event.markets.map(encodeMarket))

  private def encodeGroup(group: Group): GroupProto =
    GroupProto.defaultInstance
      .withId(group.id.value)
      .withName(group.name.value)
      .withEvents(group.events.map(encodeEvent))
      .withGroups(group.groups.map(encodeGroup))

  private def encodeRace(race: Race): RaceProto =
    RaceProto.defaultInstance
      .withId(race.id.value)
      .withName(race.name.value)
      .withCountryCode(race.countryCode.value)
      .withStartTime(race.startTime.value)
      .withMarkets(race.markets.map(encodeMarket))

  private def encodeMarket(market: Market): MarketProto =
    MarketProto.defaultInstance
      .withId(market.id.value)
      .withMarketName(market.name.value)
      .withExchangeId(market.exchangeId.value)
      .withMarketType(market.marketType.value)
      .withMarketStartTime(market.marketStartTime.value)
      .withNumberOfWinners(market.numberOfWinners.value.getOrElse(0))
}

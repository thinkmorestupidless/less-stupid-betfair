package com.thinkmorestupidless.betfair.navigation.domain

import enumeratum.{Enum, EnumEntry}
import enumeratum.EnumEntry.UpperSnakecase

sealed trait MenuItemType extends EnumEntry with UpperSnakecase
object MenuItemType extends Enum[MenuItemType] {
  override def values: IndexedSeq[MenuItemType] = findValues

  case object None extends MenuItemType
  case object Group extends MenuItemType
  case object EventType extends MenuItemType
  case object Event extends MenuItemType
  case object Race extends MenuItemType
  case object Market extends MenuItemType
}

sealed trait MenuItem
trait HasChildren {
  val children: List[MenuItem]
}

final case class MarketName(value: String)
final case class MarketId(value: String)
final case class ExchangeId(value: String)
final case class MarketType(value: String)
final case class MarketStartTime(value: String)
final case class NumberOfWinners(value: Option[Int])

final case class Market(
    id: MarketId,
    name: MarketName,
    exchangeId: ExchangeId,
    marketType: MarketType,
    marketStartTime: MarketStartTime,
    numberOfWinners: NumberOfWinners
) extends MenuItem

final case class EventName(value: String)
final case class EventId(value: String)
final case class CountryCode(value: String)

final case class Event(
    id: EventId,
    name: EventName,
    countryCode: CountryCode,
    events: List[Event],
    groups: List[Group],
    markets: List[Market]
) extends MenuItem
    with HasChildren {
  override val children: List[MenuItem] = events ++ groups ++ markets
}

final case class EventTypeName(value: String)
final case class EventTypeId(value: String)

final case class EventType(
    id: EventTypeId,
    name: EventTypeName,
    events: List[Event],
    groups: List[Group],
    races: List[Race]
) extends MenuItem
    with HasChildren {
  override val children: List[MenuItem] = events ++ groups ++ races
}

final case class GroupId(value: String)
final case class GroupName(value: String)

final case class Group(id: GroupId, name: GroupName, events: List[Event], groups: List[Group])
    extends MenuItem
    with HasChildren {
  override val children: List[MenuItem] = events ++ groups
}

final case class RaceId(value: String)
final case class RaceName(value: String)
final case class Venue(value: String)
final case class RaceStartTime(value: String)

final case class Race(
    id: RaceId,
    name: RaceName,
    countryCode: CountryCode,
    venue: Venue,
    startTime: RaceStartTime,
    markets: List[Market]
) extends MenuItem
    with HasChildren {
  override val children: List[MenuItem] = markets
}

final case class RootGroupId(value: Int)
final case class Menu(children: List[MenuItem]) extends HasChildren

final case class GetMenuRequest()

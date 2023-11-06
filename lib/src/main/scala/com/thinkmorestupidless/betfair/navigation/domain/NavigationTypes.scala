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

trait MenuItem
trait HasChildren {
  val children: List[MenuItem]
}

final case class MarketName(value: String)
final case class MarketId(value: String)
final case class ExchangeId(value: String)
final case class MarketType(value: String)
object MarketType {
  val MatchOdds = MarketType("MATCH_ODDS")
}
final case class MarketStartTime(value: String)
final case class NumberOfWinners(value: Option[Int])

final case class Market(
                         id: MarketId,
                         name: MarketName,
                         exchangeId: ExchangeId,
                         marketType: MarketType,
                         marketStartTime: MarketStartTime,
                         numberOfWinners: NumberOfWinners)
  extends MenuItem

final case class EventName(value: String)
object EventName {
  val EnglishPremierLeague = EventName("English Premier League")
}

final case class EventId(value: String)
final case class CountryCode(value: String)

final case class Event(id: EventId, name: EventName, countryCode: CountryCode, children: List[MenuItem])
  extends MenuItem
    with HasChildren

final case class EventTypeName(value: String)
final case class EventTypeId(value: String)

final case class EventType(id: EventTypeId, name: EventTypeName, children: List[MenuItem])
  extends MenuItem
    with HasChildren

final case class GroupId(value: String)
final case class GroupName(value: String)

final case class Group(id: GroupId, name: GroupName, children: List[MenuItem]) extends MenuItem with HasChildren

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
                       children: List[Market])
  extends MenuItem

final case class RootGroupId(value: Int)
final case class Menu(children: List[MenuItem]) extends HasChildren

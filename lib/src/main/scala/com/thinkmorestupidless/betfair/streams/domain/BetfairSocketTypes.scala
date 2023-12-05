package com.thinkmorestupidless.betfair.streams.domain

import com.thinkmorestupidless.betfair.exchange.domain.{EventId, EventTypeId}
import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.{CirceEnum, Enum, EnumEntry}

sealed trait MarketBettingType extends EnumEntry with UpperSnakecase
object MarketBettingType extends Enum[MarketBettingType] with CirceEnum[MarketBettingType] {
  val values = findValues

  case object Odds extends MarketBettingType
  case object Line extends MarketBettingType
  case object Range extends MarketBettingType
  case object AsianHandicapDoubleLine extends MarketBettingType
  case object AsianHandicapSingleLine extends MarketBettingType
  case object FixedOdds extends MarketBettingType
}

sealed trait MarketStatus extends EnumEntry with UpperSnakecase
object MarketStatus extends Enum[MarketStatus] with CirceEnum[MarketStatus] {
  val values = findValues

  case object Inactive extends MarketStatus
  case object Open extends MarketStatus
  case object Suspended extends MarketStatus
  case object Closed extends MarketStatus
}

sealed trait BettingType extends EnumEntry with UpperSnakecase
object BettingType extends Enum[BettingType] with CirceEnum[BettingType] {
  val values = findValues

  case object Odds extends BettingType
  case object Line extends BettingType
  case object Range extends BettingType
  case object AsianHandicapDoubleLine extends BettingType
  case object AsianHandicapSingleLine extends BettingType
}

sealed trait PriceLadderType extends EnumEntry with UpperSnakecase
object PriceLadderType extends Enum[PriceLadderType] with CirceEnum[PriceLadderType] {
  val values = findValues

  case object Classic extends PriceLadderType
  case object Finest extends PriceLadderType
  case object LineRange extends PriceLadderType
}

sealed trait RunnerStatus extends EnumEntry with UpperSnakecase
object RunnerStatus extends Enum[RunnerStatus] with CirceEnum[RunnerStatus] {
  val values = findValues

  case object Active extends RunnerStatus
  case object Removed extends RunnerStatus
  case object Winner extends RunnerStatus
  case object Placed extends RunnerStatus
  case object Loser extends RunnerStatus
  case object Hidden extends RunnerStatus
  case object RemovedVacant extends RunnerStatus
}

final case class MarketId(value: String)

final case class MarketFilter(
    marketIds: Option[List[MarketId]],
    bspMarket: Option[Boolean],
    bettingTypes: Option[List[MarketBettingType]],
    eventTypeIds: Option[List[String]],
    eventIds: Option[List[String]],
    turnInPlayEnabled: Option[Boolean],
    marketTypes: Option[List[String]],
    venues: Option[List[String]],
    countryCodes: Option[List[String]],
    raceTypes: Option[List[String]]
)

object MarketFilter {

  val empty: MarketFilter = MarketFilter(List.empty)

  def apply(marketIds: List[MarketId]): MarketFilter =
    new MarketFilter(
      marketIds = Some(marketIds),
      bspMarket = None,
      bettingTypes = None,
      eventTypeIds = None,
      eventIds = None,
      turnInPlayEnabled = None,
      marketTypes = None,
      venues = None,
      countryCodes = None,
      raceTypes = None
    )

  def withEventTypeId(eventTypeId: EventTypeId): MarketFilter =
    withEventTypeIds(Set(eventTypeId))

  def withEventTypeIds(eventTypeIds: Set[EventTypeId]): MarketFilter = {
    val maybeEventTypesIds = eventTypeIds.map(_.value).toList match {
      case Nil  => None
      case list => Some(list)
    }
    new MarketFilter(
      marketIds = None,
      bspMarket = None,
      bettingTypes = None,
      eventTypeIds = maybeEventTypesIds,
      eventIds = None,
      turnInPlayEnabled = None,
      marketTypes = None,
      venues = None,
      countryCodes = None,
      raceTypes = None
    )
  }

  def withEventId(eventId: EventId): MarketFilter =
    withEventIds(Set(eventId))

  def withEventIds(eventIds: Set[EventId]): MarketFilter = {
    val maybeEventIds = eventIds.map(_.value).toList match {
      case Nil  => None
      case list => Some(list)
    }
    new MarketFilter(
      marketIds = None,
      bspMarket = None,
      bettingTypes = None,
      eventTypeIds = None,
      eventIds = maybeEventIds,
      turnInPlayEnabled = None,
      marketTypes = None,
      venues = None,
      countryCodes = None,
      raceTypes = None
    )
  }
}

final case class MarketChange(
    rc: Option[List[RunnerChange]],
    img: Option[Boolean],
    tv: Option[BigDecimal],
    con: Option[Boolean],
    marketDefinition: Option[MarketDefinition],
    id: MarketId
)
final case class RunnerChange(
    tv: Option[BigDecimal],
    batb: Option[List[List[BigDecimal]]],
    spb: Option[List[List[BigDecimal]]],
    bdatl: Option[List[List[BigDecimal]]],
    trd: Option[List[List[BigDecimal]]],
    spf: Option[BigDecimal],
    ltp: Option[BigDecimal],
    atb: Option[List[List[BigDecimal]]],
    spl: Option[List[List[BigDecimal]]],
    spn: Option[BigDecimal],
    atl: Option[List[List[BigDecimal]]],
    batl: Option[List[List[BigDecimal]]],
    id: Long,
    hc: Option[BigDecimal],
    bdatb: Option[List[List[BigDecimal]]]
)
final case class MarketDefinition(
    status: MarketStatus,
    venue: Option[String],
    settledTime: Option[String],
    timezone: String,
    eachWayDivisor: Option[BigDecimal],
    regulators: List[String],
    marketType: String,
    marketBaseRate: BigDecimal,
    numberOfWinners: Int,
    countryCode: String,
    lineMaxUnit: Option[BigDecimal],
    inPlay: Boolean,
    betDelay: Int,
    bspMarket: Boolean,
    bettingType: BettingType,
    numberOfActiveRunners: Int,
    lineMinUnit: Option[BigDecimal],
    eventId: String,
    crossMatching: Boolean,
    runnersVoidable: Boolean,
    turnInPlayEnabled: Boolean,
    priceLadderDefinition: PriceLadderDefinition,
    keyLineDefinition: Option[KeyLineDefinition],
    suspendTime: String,
    discountAllowed: Boolean,
    persistenceEnabled: Boolean,
    runners: List[RunnerDefinition],
    version: Long,
    eventTypeId: String,
    complete: Boolean,
    openDate: String,
    marketTime: String,
    bspReconciled: Boolean,
    lineInterval: Option[BigDecimal]
)
final case class PriceLadderDefinition(`type`: PriceLadderType)
final case class KeyLineDefinition(kl: KeyLineSelection)
final case class KeyLineSelection(id: Long, hc: BigDecimal)
final case class RunnerDefinition(
    sortPriority: Int,
    removalDate: Option[String],
    id: Long,
    hc: Option[BigDecimal],
    adjustmentFactor: Option[BigDecimal],
    bsp: Option[BigDecimal],
    status: RunnerStatus
)

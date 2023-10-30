package com.thinkmorestupidless.betfair.exchange.domain

import enumeratum.{CirceEnum, Enum, EnumEntry}
import enumeratum.EnumEntry.UpperSnakecase
import com.thinkmorestupidless.betfair.core.domain.{Money, Price}

import java.time.Instant

case class EventTypeId(value: String)
object EventTypeId {
  val HorseRacing = EventTypeId("7")
}

case class EventTypeName(value: String)
case class EventType(id: EventTypeId, name: EventTypeName)

case class EventId(value: String)
case class EventName(value: String)
case class CountryCode(value: String)
case class Timezone(value: String)
case class Venue(value: String)
case class OpenDate(value: Instant)
case class CancelledDate(value: Instant)

case class Event(
    id: EventId,
    name: EventName,
    countryCode: Option[CountryCode],
    timezone: Timezone,
    venue: Option[Venue],
    openDate: OpenDate
)

case class TimeRange(from: Instant, to: Instant)

sealed trait BetStatus extends EnumEntry with UpperSnakecase
object BetStatus extends Enum[BetStatus] with CirceEnum[BetStatus] {
  val values = findValues

  case object Settled extends BetStatus
  case object Voided extends BetStatus
  case object Lapsed extends BetStatus
  case object Cancelled extends BetStatus
}

sealed trait GroupBy extends EnumEntry with UpperSnakecase
object GroupBy extends Enum[GroupBy] with CirceEnum[GroupBy] {
  val values = findValues

  case object EventType extends GroupBy
  case object Event extends GroupBy
  case object Market extends GroupBy
  case object Side extends GroupBy
  case object Bet extends GroupBy
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

sealed trait OrderBy extends EnumEntry with UpperSnakecase
object OrderBy extends Enum[OrderBy] with CirceEnum[OrderBy] {
  val values = findValues

  case object byBet extends OrderBy
  case object byMarket extends OrderBy
  case object byMatchTime extends OrderBy
  case object byPlaceTime extends OrderBy
  case object bySettledTime extends OrderBy
  case object byVoidTime extends OrderBy
}

sealed trait OrderStatus extends EnumEntry with UpperSnakecase
object OrderStatus extends Enum[OrderStatus] with CirceEnum[OrderStatus] {
  val values = findValues

  case object ExecutionComplete extends MarketBettingType
  case object Executable extends MarketBettingType
}

sealed trait MarketProjection extends EnumEntry with UpperSnakecase
object MarketProjection extends Enum[MarketProjection] with CirceEnum[MarketProjection] {
  val values = findValues

  case object Competition extends MarketProjection
  case object Event extends MarketProjection
  case object EventType extends MarketProjection
  case object MarketStartTime extends MarketProjection
  case object MarketDescription extends MarketProjection
  case object RunnerDescription extends MarketProjection
  case object RunnerMetadata extends MarketProjection
}

sealed trait MarketSort extends EnumEntry with UpperSnakecase
object MarketSort extends Enum[MarketSort] with CirceEnum[MarketSort] {
  val values = findValues

  case object MinimumTraded extends MarketSort
  case object MaximumTraded extends MarketSort
  case object MinimumAvailable extends MarketSort
  case object MaximumAvailable extends MarketSort
  case object FirstToStart extends MarketSort
  case object LastToStart extends MarketSort
}

sealed trait SortDir extends EnumEntry with UpperSnakecase
object SortDir extends Enum[SortDir] with CirceEnum[SortDir] {
  val values = findValues

  case object earliestToLatest extends SortDir
  case object latestToEarliest extends SortDir
}

case class CountryCodeResult(countryCode: CountryCode, marketCount: Int)

case class TextQuery(value: String)
case class MarketId(value: String)
case class InPlayOnly(value: Boolean)
case class CompetitionId(value: String)
case class BspOnly(value: Boolean)
case class TurnInPlayEnabled(value: Boolean)
case class MarketCountry(value: String)
case class MarketTypeCode(value: String)

case class MarketFilter(
    textQuery: Option[TextQuery] = None,
    eventTypeIds: Option[Set[EventTypeId]] = None,
    marketIds: Option[Set[MarketId]] = None,
    inPlayOnly: Option[InPlayOnly] = None,
    eventIds: Option[Set[EventId]] = None,
    competitionIds: Option[Set[CompetitionId]] = None,
    venues: Option[Set[Venue]] = None,
    bspOnly: Option[BspOnly] = None,
    turnInPlayEnabled: Option[TurnInPlayEnabled] = None,
    marketBettingTypes: Option[Set[MarketBettingType]] = None,
    marketCountries: Option[Set[MarketCountry]] = None,
    marketTypeCodes: Option[Set[MarketTypeCode]] = None,
    marketStartTime: Option[TimeRange] = None,
    withOrders: Option[Set[OrderStatus]] = None
)

case class PriceSize(price: Price, size: Money)

case class ExchangePrices(
    availableToBack: List[PriceSize],
    availableToLay: List[PriceSize],
    tradedVolume: List[PriceSize]
)

case class NearPrice(value: Price)
case class FarPrice(value: Price)
case class StartingPrice(value: Price)

case class StartingPrices(
    nearPrice: NearPrice,
    farPrice: FarPrice,
    backStakeTaken: List[PriceSize],
    layLiabilityTaken: List[PriceSize],
    actualSP: StartingPrice
)

case class BetId(value: String)
case class BspLiability(value: BigDecimal)
case class AveragePriceMatched(value: Price)
case class CustomerOrderRef(value: String)
case class CustomerStrategyRef(value: String)
case class CurrencyCode(value: String)
case class IncludeOverallPosition(value: Boolean)
case class PartitionMatchedByStrategyRef(value: Boolean)
case class MarketVersion(value: Long)
case class CurrentItemDescription(marketVersion: MarketVersion)
case class SizeMatched(value: Money)
case class SizeRemaining(value: Money)
case class SizeLapsed(value: Money)
case class SizeCancelled(value: Money)
case class SizeVoided(value: Money)
case class PlacedDate(value: Instant)

case class Order(
    betId: BetId,
    orderType: OrderType,
    status: OrderStatus,
    persistenceType: PersistenceType,
    side: Side,
    price: Price,
    size: Money,
    bspLiability: BspLiability,
    placedDate: PlacedDate,
    avgPriceMatched: AveragePriceMatched,
    sizeMatched: SizeMatched,
    sizeRemaining: SizeRemaining,
    sizeLapsed: SizeLapsed,
    sizeCancelled: SizeCancelled,
    sizeVoided: SizeVoided
)

case class MatchId(value: String)
case class MatchDate(value: Instant)

case class Match(betId: BetId, matchId: MatchId, side: Side, price: Price, size: Money, matchDate: MatchDate)

case class SelectionId(value: Long)
case class Handicap(value: BigDecimal)
case class AdjustmentFactor(value: BigDecimal)
case class LastPriceTraded(value: Price)
case class TotalMatched(value: BigDecimal)
case class RemovalDate(value: Instant)

case class Runner(
    selectionId: SelectionId,
    handicap: Handicap,
    status: RunnerStatus,
    adjustmentFactor: Option[AdjustmentFactor],
    lastPriceTraded: Option[LastPriceTraded],
    totalMatched: Option[TotalMatched],
    removalDate: Option[RemovalDate],
    sp: Option[StartingPrices],
    ex: Option[ExchangePrices],
    orders: Option[List[Order]],
    matches: Option[List[Match]]
)

case class TotalAvailable(value: BigDecimal)
case class LastMatchTime(value: Instant)

case class MarketBook(
    marketId: MarketId,
    isMarketDataDelayed: Boolean,
    status: String,
    betDelay: Int,
    bspReconciled: Boolean,
    complete: Boolean,
    inplay: Boolean,
    numberOfWinners: Int,
    numberOfRunners: Int,
    numberOfActiveRunners: Int,
    lastMatchTime: Option[LastMatchTime],
    totalMatched: TotalMatched,
    totalAvailable: TotalAvailable,
    crossMatching: Boolean,
    runnersVoidable: Boolean,
    version: Long,
    runners: List[Runner]
)

case class MarketTime(value: Instant)
case class SuspendTime(value: Instant)
case class SettleTime(value: Instant)

case class MarketDescription(
    persistenceEnabled: Boolean,
    bspMarket: Boolean,
    marketTime: MarketTime,
    suspendTime: SuspendTime,
    settleTime: SettleTime,
    bettingType: String,
    turnInPlayEnabled: Boolean,
    marketType: String,
    regulator: String,
    marketBaseRate: BigDecimal,
    discountAllowed: Boolean,
    wallet: String,
    rules: String,
    rulesHasDate: Boolean,
    clarifications: String
)

case class SortPriority(value: Int)
case class RunnerName(value: String)
case class RunnerMetadata(value: Map[String, String])

case class RunnerCatalog(
    sortPriority: Int,
    selectionId: Long,
    runnerName: String,
    handicap: BigDecimal,
    metadata: Option[RunnerMetadata]
)

case class CompetitionName(value: String)
case class CompetitionRegion(value: String)
case class Competition(id: CompetitionId, name: CompetitionName)
case class CompetitionResult(competition: Competition, marketCount: Int, competitionRegion: CompetitionRegion)

case class MarketName(value: String)
case class MarketStartTime(value: Instant)

case class MarketCatalogue(
    marketId: MarketId,
    marketName: MarketName,
    marketStartTime: Option[MarketStartTime],
    description: Option[MarketDescription],
    runners: Option[List[RunnerCatalog]],
    eventType: Option[EventType],
    competition: Option[Competition],
    event: Option[Event],
    totalMatched: TotalMatched
)

sealed trait PriceData extends EnumEntry with UpperSnakecase
object PriceData extends Enum[PriceData] with CirceEnum[PriceData] {
  val values = findValues

  case object SpAvailable extends PriceData
  case object SpTraded extends PriceData
  case object ExBestOffers extends PriceData
  case object ExAllOffers extends PriceData
  case object ExTraded extends PriceData
}

sealed trait RollupModel extends EnumEntry with UpperSnakecase
object RollupModel extends Enum[RollupModel] with CirceEnum[RollupModel] {
  val values = findValues

  case object Stake extends RollupModel
  case object Payout extends RollupModel
  case object ManagedLiability extends RollupModel
  case object None extends RollupModel
}

sealed trait OrderProjection extends EnumEntry with UpperSnakecase
object OrderProjection extends Enum[OrderProjection] with CirceEnum[OrderProjection] {
  val values = findValues

  case object All extends OrderProjection
  case object Executable extends OrderProjection
  case object ExecutionComplete extends OrderProjection
}

sealed trait MatchProjection extends EnumEntry with UpperSnakecase
object MatchProjection extends Enum[MatchProjection] with CirceEnum[MatchProjection] {
  val values = findValues

  case object NoRollup extends MatchProjection
  case object RolledUpByPrice extends MatchProjection
  case object RolledUpByAveragePrice extends MatchProjection
}

case class ExBestOffersOverrides(
    bestPricesDepth: Int,
    rollupModel: RollupModel,
    rollupLimit: Int,
    rollupLiabilityThreshold: BigDecimal,
    rollupLiabilityFactor: Int
)

case class Virtualise(value: Boolean)
case class RolloverStakes(value: Boolean)

case class PriceProjection(
    priceData: Set[PriceData],
    exBestOffersOverrides: ExBestOffersOverrides,
    virtualise: Virtualise,
    rolloverStakes: RolloverStakes
)

sealed trait OrderType extends EnumEntry with UpperSnakecase
object OrderType extends Enum[OrderType] with CirceEnum[OrderType] {
  val values = findValues

  case object Limit extends OrderType
  case object LimitOnClose extends OrderType
  case object MarketOnClose extends OrderType
}

sealed trait Side extends EnumEntry with UpperSnakecase
object Side extends Enum[Side] with CirceEnum[Side] {
  val values = findValues

  case object Back extends Side
  case object Lay extends Side
}

sealed trait PersistenceType extends EnumEntry with UpperSnakecase
object PersistenceType extends Enum[PersistenceType] with CirceEnum[PersistenceType] {
  val values = findValues

  case object Lapse extends PersistenceType
  case object Persist extends PersistenceType
  case object MarketOnClose extends PersistenceType
}

sealed trait TimeInForce extends EnumEntry with UpperSnakecase
object TimeInForce extends Enum[TimeInForce] with CirceEnum[TimeInForce] {
  val values = findValues

  case object FillOrKill extends TimeInForce
}

case class MinimumFillSize(value: BigDecimal)

case class LimitOrder(
    size: Money,
    price: Price,
    persistenceType: PersistenceType,
    timeInForce: Option[TimeInForce],
    minFillSize: Option[MinimumFillSize]
)

case class Liability(value: BigDecimal)
case class LimitOnCloseOrder(liability: Liability, price: Price)
case class MarketOnCloseOrder(liability: Liability)
case class RegulatorAuthCode(value: String)
case class RegulatorCode(value: String)

case class PlaceInstruction(
    orderType: OrderType,
    selectionId: SelectionId,
    handicap: Option[Handicap],
    side: Side,
    limitOrder: Option[LimitOrder],
    limitOnCloseOrder: Option[LimitOnCloseOrder],
    marketOnCloseOrder: Option[MarketOnCloseOrder],
    customerOrderRef: Option[String]
)

sealed trait ExecutionReportStatus extends EnumEntry with UpperSnakecase
object ExecutionReportStatus extends Enum[ExecutionReportStatus] with CirceEnum[ExecutionReportStatus] {
  val values = findValues

  case object Success extends ExecutionReportStatus
  case object Failure extends ExecutionReportStatus
  case object ProcessedWithErrors extends ExecutionReportStatus
  case object Timeout extends ExecutionReportStatus
}

sealed trait ExecutionReportErrorCode extends EnumEntry with UpperSnakecase
object ExecutionReportErrorCode extends Enum[ExecutionReportErrorCode] with CirceEnum[ExecutionReportErrorCode] {
  val values = findValues

  case object ErrorInMatcher extends ExecutionReportErrorCode
  case object FaiProcessedWithErrorslure extends ExecutionReportErrorCode
  case object BetActionError extends ExecutionReportErrorCode
  case object InvalidAccountState extends ExecutionReportErrorCode
  case object InvalidWalletStatus extends ExecutionReportErrorCode
  case object InsufficientFunds extends ExecutionReportErrorCode
  case object LossLimitExceeded extends ExecutionReportErrorCode
  case object MarketSuspended extends ExecutionReportErrorCode
  case object MarketNotOpenForBetting extends ExecutionReportErrorCode
  case object DuplicateTransaction extends ExecutionReportErrorCode
  case object InvalidOrder extends ExecutionReportErrorCode
  case object InvalidMarketId extends ExecutionReportErrorCode
  case object PermissionDenied extends ExecutionReportErrorCode
  case object DuplicateBetids extends ExecutionReportErrorCode
  case object NoActionRequired extends ExecutionReportErrorCode
  case object ServiceUnavailable extends ExecutionReportErrorCode
  case object RejectedByRegulator extends ExecutionReportErrorCode
  case object NoChasing extends ExecutionReportErrorCode
  case object RegulatorIsNotAvailable extends ExecutionReportErrorCode
  case object TooManyInstructions extends ExecutionReportErrorCode
  case object InvalidMarketVersion extends ExecutionReportErrorCode
}

sealed trait InstructionReportStatus extends EnumEntry with UpperSnakecase
object InstructionReportStatus extends Enum[InstructionReportStatus] with CirceEnum[InstructionReportStatus] {
  val values = findValues

  case object Success extends InstructionReportStatus
  case object Failure extends InstructionReportStatus
  case object Timeout extends InstructionReportStatus
}

sealed trait InstructionReportErrorCode extends EnumEntry with UpperSnakecase {
  val description: String
}
object InstructionReportErrorCode extends Enum[InstructionReportErrorCode] with CirceEnum[InstructionReportErrorCode] {
  val values = findValues

  case object InvalidBetSize extends InstructionReportErrorCode {
    val description = "Bet size is invalid for your currency or your regulator"
  }
  case object InvalidRunner extends InstructionReportErrorCode {
    val description = "Runner does not exist, includes vacant traps in greyhound racing"
  }
  case object BetTakenOrLapsed extends InstructionReportErrorCode {
    val description =
      "Bet cannot be cancelled or modified as it has already been taken or has been cancelled/lapsed Includes attempts to cancel/modify market on close BSP bets and cancelling limit on close BSP bets. The error may be returned on placeOrders request if for example a bet is placed at the point when a market admin event takes place (i.e. market is turned in-play)"
  }
  case object BetInProgress extends InstructionReportErrorCode {
    val description = "No result was received from the matcher in a timeout configured for the system"
  }
  case object RunnerRemoved extends InstructionReportErrorCode {
    val description = "Runner has been removed from the event"
  }
  case object MarketNotOpenForBetting extends InstructionReportErrorCode {
    val description = "Attempt to edit a bet on a market that has closed."
  }
  case object LossLimitExceeded extends InstructionReportErrorCode {
    val description = "The action has caused the account to exceed the self imposed loss limit"
  }
  case object MarketNotOpenForBspBetting extends InstructionReportErrorCode {
    val description = "Market now closed to bsp betting. Turned in-play or has been reconciled"
  }
  case object InvalidPriceEdit extends InstructionReportErrorCode {
    val description =
      "Attempt to edit down the price of a bsp limit on close lay bet, or edit up the price of a limit on close back bet"
  }
  case object InvalidOdds extends InstructionReportErrorCode {
    val description = "Odds not on price ladder - either edit or placement"
  }
  case object InsufficientFunds extends InstructionReportErrorCode {
    val description =
      "Insufficient funds available to cover the bet action. Either the exposure limit or available to bet limit would be exceeded"
  }
  case object InvalidPersistenceType extends InstructionReportErrorCode {
    val description = "Invalid persistence type for this market, e.g. KEEP for a non in-play market."
  }
  case object ErrorInMatcher extends InstructionReportErrorCode {
    val description = "A problem with the matcher prevented this action completing successfully"
  }
  case object InvalidBackLayCombination extends InstructionReportErrorCode {
    val description =
      "The order contains a back and a lay for the same runner at overlapping prices. This would guarantee a self match. This also applies to BSP limit on close bets"
  }
  case object ErrorInOrder extends InstructionReportErrorCode {
    val description = "The action failed because the parent order failed"
  }
  case object InvalidBidType extends InstructionReportErrorCode {
    val description = "Bid type is mandatory"
  }
  case object InvalidBetId extends InstructionReportErrorCode {
    val description = "Bet for id supplied has not been found"
  }
  case object CancelledNotPlaced extends InstructionReportErrorCode {
    val description = "Bet cancelled but replacement bet was not placed"
  }
  case object RelatedActionFailed extends InstructionReportErrorCode {
    val description = "Action failed due to the failure of a action on which this action is dependent"
  }
  case object NoActionRequired extends InstructionReportErrorCode {
    val description = "The action does not result in any state change. eg changing a persistence to it's current value"
  }
  case object TimeInForceConflict extends InstructionReportErrorCode {
    val description =
      "You may only specify a time in force on either the place request OR on individual limit order instructions (not both), since the implied behaviors are incompatible."
  }
  case object UnexpectedPersistenceType extends InstructionReportErrorCode {
    val description =
      "You have specified a persistence type for a FILL_OR_KILL order, which is nonsensical because no umatched portion can remain after the order has been placed."
  }
  case object InvalidOrderType extends InstructionReportErrorCode {
    val description = "You have specified a time in force of FILL_OR_KILL, but have included a non-LIMIT order type."
  }
  case object UnexpectedMinFillSize extends InstructionReportErrorCode {
    val description =
      "You have specified a minFillSize on a limit order, where the limit order's time in force is not FILL_OR_KILL. Using minFillSize is not supported where the time in force of the request (as opposed to an order) is FILL_OR_KILL."
  }
  case object InvalidCustomerOrderRef extends InstructionReportErrorCode {
    val description = "The supplied customer order reference is too long."
  }
  case object InvalidMinFillSize extends InstructionReportErrorCode {
    val description =
      "The minFillSize must be greater than zero and less than or equal to the order's size. The minFillSize cannot be less than the minimum bet size for your currency"
  }
  case object BetLapsedPriceImprovementTooLarge extends InstructionReportErrorCode {
    val description =
      "Your bet is lapsed. There is better odds than requested available in the market, but your preferences don't allow the system to match your bet against better odds. Change your betting preferences to accept better odds if you don't want to receive this error."
  }
}

case class PlaceInstructionReport(
    status: InstructionReportStatus,
    errorCode: Option[InstructionReportErrorCode],
    orderStatus: Option[OrderStatus],
    instruction: PlaceInstruction,
    betId: Option[BetId],
    placedDate: Option[PlacedDate],
    averageMatchedPrice: Option[Price],
    sizeMatched: Option[Money]
)

case class CustomerRef(value: String)

case class PlaceExecutionReport(
    customerRef: Option[CustomerRef],
    status: ExecutionReportStatus,
    errorCode: Option[ExecutionReportErrorCode],
    marketId: MarketId,
    instructionReports: List[PlaceInstructionReport]
)

case class PlaceOrders(
    marketId: MarketId,
    instructions: List[PlaceInstruction],
    customerRef: Option[String],
    marketVersion: Option[String],
    customerStrategyRef: Option[String],
    async: Boolean
)

case class CurrentOrderSummaryReport(currentOrders: List[CurrentOrderSummary], moreAvailable: Boolean)

case class CurrentOrderSummary(
    betId: BetId,
    marketId: MarketId,
    selectionId: SelectionId,
    handicap: Handicap,
    priceSize: PriceSize,
    bspLiability: BspLiability,
    side: Side,
    status: OrderStatus,
    persistenceType: PersistenceType,
    orderType: OrderType,
    placedDate: PlacedDate,
    matchedDate: MatchDate,
    averagePriceMatched: AveragePriceMatched,
    sizeMatched: SizeMatched,
    sizeRemaining: SizeRemaining,
    sizeLapsed: SizeLapsed,
    sizeCancelled: SizeCancelled,
    sizeVoided: SizeVoided,
    regulatorAuthCode: RegulatorAuthCode,
    regulatorCode: RegulatorCode,
    customerOrderRef: CustomerOrderRef,
    customerStrategyRef: CustomerStrategyRef,
    currentItemDescription: CurrentItemDescription
)

case class SizeReduction(value: Money)
case class CancelInstruction(betId: BetId, sizeReduction: Option[SizeReduction])
case class CancelExecutionReport(
    marketId: MarketId,
    status: ExecutionReportStatus,
    customerRef: CustomerRef,
    errorCode: ExecutionReportErrorCode,
    instructionReports: List[CancelInstructionReport]
)
case class CancelInstructionReport(
    status: InstructionReportStatus,
    errorCode: InstructionReportErrorCode,
    instruction: CancelInstruction,
    sizeCancelled: SizeCancelled,
    cancelledDate: CancelledDate
)

case class RunnerId(marketId: MarketId, selectionId: SelectionId, handicap: Handicap)
case class EventTypeDesc(value: String)
case class EventDesc(value: String)
case class MarketDesc(value: String)
case class MarketType(value: String)
case class RunnerDescription(value: String)
case class NumberOfWinners(value: Int)
case class EachWayDivisor(value: BigDecimal)
case class SettledDate(value: Instant)
case class LastMatchedDate(value: Instant)
case class ItemDescription(
    eventTypeDesc: EventTypeDesc,
    eventDesc: EventDesc,
    marketDesc: MarketDesc,
    marketType: MarketType,
    marketStartTime: MarketStartTime,
    runnerDesc: RunnerDescription,
    numberOfWinners: NumberOfWinners,
    eachWayDivisor: EachWayDivisor
)

case class ClearedOrderSummary(
    eventTypeId: EventTypeId,
    eventId: EventId,
    marketId: MarketId,
    selectionId: SelectionId,
    handicap: Handicap,
    betId: BetId,
    placedDate: PlacedDate,
    persistenceType: PersistenceType,
    orderType: OrderType,
    side: Side,
    itemDescription: ItemDescription,
    betOutcome: String,
    priceRequested: Price,
    settledDate: SettledDate,
    lastMatchedDate: LastMatchedDate,
    betCount: Int,
    commission: Money,
    priceMatched: Price,
    priceReduced: Boolean,
    sizeSettled: Money,
    profit: Money,
    sizeCancelled: SizeCancelled,
    customerOrderRef: String,
    customerStrategyRef: String
)

case class ClearedOrderSummaryReport(
    clearedOrders: List[ClearedOrderSummary],
    moreAvailable: Boolean
)

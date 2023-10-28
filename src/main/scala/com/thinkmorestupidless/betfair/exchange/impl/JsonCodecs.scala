package com.thinkmorestupidless.betfair.exchange.impl

import io.circe.Codec
import io.circe.generic.semiauto._
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService._
import com.thinkmorestupidless.betfair.exchange.domain._
import com.thinkmorestupidless.utils.CirceUtils._
import com.thinkmorestupidless.betfair.utils.BettingCirceUtils._

object JsonCodecs {

  implicit val adjustmentFactorCodec: Codec[AdjustmentFactor] = bimapDecimal(_.value, AdjustmentFactor(_))
  implicit val avgPriceMatchedCodec: Codec[AveragePriceMatched] = bimapPrice(_.value, AveragePriceMatched(_))
  implicit val betIdCodec: Codec[BetId] = bimapString(_.value, BetId(_))
  implicit val bspLiabilityCodec: Codec[BspLiability] = bimapDecimal(_.value, BspLiability(_))
  implicit val bspOnlyCodec: Codec[BspOnly] = bimapBoolean(_.value, BspOnly(_))
  implicit val cancelledDateCodec: Codec[CancelledDate] = bimapInstant(_.value, CancelledDate(_))
  implicit val competitionIdCodec: Codec[CompetitionId] = bimapString(_.value, CompetitionId(_))
  implicit val competitionNameCodec: Codec[CompetitionName] = bimapString(_.value, CompetitionName(_))
  implicit val competitionRegionCodec: Codec[CompetitionRegion] = bimapString(_.value, CompetitionRegion(_))
  implicit val countryCodeCodec: Codec[CountryCode] = bimapString(_.value, CountryCode(_))
  implicit val currencyCodeCodec: Codec[CurrencyCode] = bimapString(_.value, CurrencyCode(_))
  implicit val customerOrderRefCodec: Codec[CustomerOrderRef] = bimapString(_.value, CustomerOrderRef(_))
  implicit val customerRefCodec: Codec[CustomerRef] = bimapString(_.value, CustomerRef(_))
  implicit val customerStrategyRefCodec: Codec[CustomerStrategyRef] = bimapString(_.value, CustomerStrategyRef(_))
  implicit val eachWayDivisorCodec: Codec[EachWayDivisor] = bimapDecimal(_.value, EachWayDivisor(_))
  implicit val eventDescCodec: Codec[EventDesc] = bimapString(_.value, EventDesc(_))
  implicit val eventIdCodec: Codec[EventId] = bimapString(_.value, EventId(_))
  implicit val eventNameCodec: Codec[EventName] = bimapString(_.value, EventName(_))
  implicit val eventTypeDescCodec: Codec[EventTypeDesc] = bimapString(_.value, EventTypeDesc(_))
  implicit val eventTypeNameCodec: Codec[EventTypeName] = bimapString(_.value, EventTypeName(_))
  implicit val eventTypeIdCodec: Codec[EventTypeId] = bimapString(_.value, EventTypeId(_))
  implicit val farPriceCodec: Codec[FarPrice] = bimapPrice(_.value, FarPrice(_))
  implicit val handicapCodec: Codec[Handicap] = bimapDecimal(_.value, Handicap(_))
  implicit val includeOverallPositionCodec: Codec[IncludeOverallPosition] =
    bimapBoolean(_.value, IncludeOverallPosition(_))
  implicit val inPlayOnlyCodec: Codec[InPlayOnly] = bimapBoolean(_.value, InPlayOnly(_))
  implicit val lastMatchedDate: Codec[LastMatchedDate] = bimapInstant(_.value, LastMatchedDate(_))
  implicit val lastMatchTimeCodec: Codec[LastMatchTime] = bimapInstant(_.value, LastMatchTime(_))
  implicit val lastPriceTradedCodec: Codec[LastPriceTraded] = bimapPrice(_.value, LastPriceTraded(_))
  implicit val liabilityCodec: Codec[Liability] = bimapDecimal(_.value, Liability(_))
  implicit val limitOnCloseOrderCodec: Codec[LimitOnCloseOrder] = deriveCodec[LimitOnCloseOrder]
  implicit val limitOrderCodec: Codec[LimitOrder] = deriveCodec[LimitOrder]
  implicit val localeCodec: Codec[Locale] = bimapString(_.value, Locale(_))
  implicit val marketCountriesCodec: Codec[MarketCountry] = bimapString(_.value, MarketCountry(_))
  implicit val marketDesCodec: Codec[MarketDesc] = bimapString(_.value, MarketDesc(_))
  implicit val marketIdCodec: Codec[MarketId] = bimapString(_.value, MarketId(_))
  implicit val marketNameCodec: Codec[MarketName] = bimapString(_.value, MarketName(_))
  implicit val marketOnCloseOrderCodec: Codec[MarketOnCloseOrder] = deriveCodec[MarketOnCloseOrder]
  implicit val marketStartTimeCodec: Codec[MarketStartTime] = bimapInstant(_.value, MarketStartTime(_))
  implicit val marketTimeCodec: Codec[MarketTime] = bimapInstant(_.value, MarketTime(_))
  implicit val marketTypeCodec: Codec[MarketType] = bimapString(_.value, MarketType(_))
  implicit val marketTypeCodesCodec: Codec[MarketTypeCode] = bimapString(_.value, MarketTypeCode(_))
  implicit val marketVersionCodec: Codec[MarketVersion] = bimapLong(_.value, MarketVersion(_))
  implicit val matchedSinceCodec: Codec[MatchedSince] = bimapInstant(_.value, MatchedSince(_))
  implicit val matchIdCodec: Codec[MatchId] = bimapString(_.value, MatchId(_))
  implicit val matchDateCodec: Codec[MatchDate] = bimapInstant(_.value, MatchDate(_))
  implicit val maxResultsCodec: Codec[MaxResults] = bimapInt(_.value, MaxResults(_))
  implicit val minimumFillSizeCodec: Codec[MinimumFillSize] = bimapDecimal(_.value, MinimumFillSize(_))
  implicit val nearPriceCodec: Codec[NearPrice] = bimapPrice(_.value, NearPrice(_))
  implicit val numberOfWinnersCodec: Codec[NumberOfWinners] = bimapInt(_.value, NumberOfWinners(_))
  implicit val openDateCodec: Codec[OpenDate] = bimapInstant(_.value, OpenDate(_))
  implicit val partitionMatchedByStrategyRefCodec: Codec[PartitionMatchedByStrategyRef] =
    bimapBoolean(_.value, PartitionMatchedByStrategyRef(_))
  implicit val placedDateCodec: Codec[PlacedDate] = bimapInstant(_.value, PlacedDate(_))
  implicit val regulatorAuthCodeCodec: Codec[RegulatorAuthCode] = bimapString(_.value, RegulatorAuthCode(_))
  implicit val regulatorCodeCodec: Codec[RegulatorCode] = bimapString(_.value, RegulatorCode(_))
  implicit val removalDateCodec: Codec[RemovalDate] = bimapInstant(_.value, RemovalDate(_))
  implicit val rolloverStakesCodec: Codec[RolloverStakes] = bimapBoolean(_.value, RolloverStakes(_))
  implicit val runnerDescriptionCodec: Codec[RunnerDescription] = bimapString(_.value, RunnerDescription(_))
  implicit val runnerMetadataCodec: Codec[RunnerMetadata] = bimapStringMap(_.value, RunnerMetadata(_))
  implicit val selectionIdCodec: Codec[SelectionId] = bimapLong(_.value, SelectionId(_))
  implicit val settledDateCoded: Codec[SettledDate] = bimapInstant(_.value, SettledDate(_))
  implicit val settleTimeCodec: Codec[SettleTime] = bimapInstant(_.value, SettleTime(_))
  implicit val sizeCancelledCodec: Codec[SizeCancelled] = bimapMoney(_.value, SizeCancelled(_))
  implicit val sizeLapsedCodec: Codec[SizeLapsed] = bimapMoney(_.value, SizeLapsed(_))
  implicit val sizeMatchedCodec: Codec[SizeMatched] = bimapMoney(_.value, SizeMatched(_))
  implicit val sizeReductionCodec: Codec[SizeReduction] = bimapMoney(_.value, SizeReduction(_))
  implicit val sizeRemainingCodec: Codec[SizeRemaining] = bimapMoney(_.value, SizeRemaining(_))
  implicit val sizeVoidedCodec: Codec[SizeVoided] = bimapMoney(_.value, SizeVoided(_))
  implicit val startingPriceCodec: Codec[StartingPrice] = bimapPrice(_.value, StartingPrice(_))
  implicit val suspendTimeCodec: Codec[SuspendTime] = bimapInstant(_.value, SuspendTime(_))
  implicit val textQueryCodec: Codec[TextQuery] = bimapString(_.value, TextQuery(_))
  implicit val timezoneCodec: Codec[Timezone] = bimapString(_.value, Timezone(_))
  implicit val totalAvailableCodec: Codec[TotalAvailable] = bimapDecimal(_.value, TotalAvailable(_))
  implicit val totalMatchedCodec: Codec[TotalMatched] = bimapDecimal(_.value, TotalMatched(_))
  implicit val turnInPlayEnabledCodec: Codec[TurnInPlayEnabled] = bimapBoolean(_.value, TurnInPlayEnabled(_))
  implicit val venueCodec: Codec[Venue] = bimapString(_.value, Venue(_))
  implicit val virtualiseCodec: Codec[Virtualise] = bimapBoolean(_.value, Virtualise(_))

  implicit val competitionCodec: Codec[Competition] = deriveCodec[Competition]
  implicit val listClearedOrdersCodec: Codec[ListClearedOrders] = deriveCodec[ListClearedOrders]
  implicit val priceSizeCodec: Codec[PriceSize] = deriveCodec[PriceSize]
  implicit val runnerIdCodec: Codec[RunnerId] = deriveCodec[RunnerId]

  implicit val cancelOrdersCodec: Codec[CancelOrders] = deriveCodec[CancelOrders]
  implicit val cancelExecutionReportCodec: Codec[CancelExecutionReport] = deriveCodec[CancelExecutionReport]
  implicit val cancelInstructionCodec: Codec[CancelInstruction] = deriveCodec[CancelInstruction]
  implicit val cancelInstructionReportCodec: Codec[CancelInstructionReport] = deriveCodec[CancelInstructionReport]
  implicit val competitionResultCodec: Codec[CompetitionResult] = deriveCodec[CompetitionResult]
  implicit val currentItemDescriptionCodec: Codec[CurrentItemDescription] = deriveCodec[CurrentItemDescription]
  implicit val currentOrderSummaryCodec: Codec[CurrentOrderSummary] = deriveCodec[CurrentOrderSummary]
  implicit val currentOrderSummaryReportCodec: Codec[CurrentOrderSummaryReport] = deriveCodec[CurrentOrderSummaryReport]
  implicit val descriptionCodec: Codec[MarketDescription] = deriveCodec[MarketDescription]
  implicit val eventCodec: Codec[Event] = deriveCodec[Event]
  implicit val eventTypeCodec: Codec[EventType] = deriveCodec[EventType]

  implicit val runnersCodec: Codec[RunnerCatalog] = deriveCodec[RunnerCatalog]

  implicit val countryCodeResultCodec: Codec[CountryCodeResult] = deriveCodec[CountryCodeResult]
  implicit val itemDescriptionCodec: Codec[ItemDescription] = deriveCodec[ItemDescription]
  implicit val listCompetitionsCodec: Codec[ListCompetitions] = deriveCodec[ListCompetitions]
  implicit val listCountriesRequestCodec: Codec[ListCountries] = deriveCodec[ListCountries]
  implicit val listCurrentOrdersCodec: Codec[ListCurrentOrders] = deriveCodec[ListCurrentOrders]

  implicit val clearedOrderSummaryCodec: Codec[ClearedOrderSummary] = deriveCodec[ClearedOrderSummary]
  implicit val clearedOrderSummaryReportCodec: Codec[ClearedOrderSummaryReport] = deriveCodec[ClearedOrderSummaryReport]
  implicit val eventResponseCodec: Codec[EventResponse] = deriveCodec[EventResponse]
  implicit val eventTypeResponseCodec: Codec[EventTypeResponse] = deriveCodec[EventTypeResponse]
  implicit val exchangePricesCodec: Codec[ExchangePrices] = deriveCodec[ExchangePrices]
  implicit val marketCatalogueCodec: Codec[MarketCatalogue] = deriveCodec[MarketCatalogue]
  implicit val matchCodec: Codec[Match] = deriveCodec[Match]
  implicit val placeInstructionCodec: Codec[PlaceInstruction] = deriveCodec[PlaceInstruction]
  implicit val placeInstructionReport: Codec[PlaceInstructionReport] = deriveCodec[PlaceInstructionReport]
  implicit val orderCodec: Codec[Order] = deriveCodec[Order]
  implicit val runnerCodec: Codec[Runner] = deriveCodec[Runner]
  implicit val startingPricesCodec: Codec[StartingPrices] = deriveCodec[StartingPrices]
  implicit val timeRangeCodec: Codec[TimeRange] = deriveCodec[TimeRange]

  implicit val exBestOffersOverridesCodec: Codec[ExBestOffersOverrides] = deriveCodec[ExBestOffersOverrides]
  implicit val listEventsCodec: Codec[ListEvents] = deriveCodec[ListEvents]
  implicit val listEventTypesCodec: Codec[ListEventTypes] = deriveCodec[ListEventTypes]
  implicit val listMarketBookCodec: Codec[ListMarketBook] = deriveCodec[ListMarketBook]
  implicit val listMarketCatalogueCodec: Codec[ListMarketCatalogue] = deriveCodec[ListMarketCatalogue]
  implicit val marketBookCodec: Codec[MarketBook] = deriveCodec[MarketBook]
  implicit val marketFilterCodec: Codec[MarketFilter] = deriveCodec[MarketFilter]
  implicit val placeExecutionReportCodec: Codec[PlaceExecutionReport] = deriveCodec[PlaceExecutionReport]
  implicit val placeOrdersCodec: Codec[PlaceOrders] = deriveCodec[PlaceOrders]
  implicit val priceProjectionCodec: Codec[PriceProjection] = deriveCodec[PriceProjection]
}

package com.thinkmorestupidless.betfair.streams.impl.grpc

import com.thinkmorestupidless.betfair.proto.streams.MarketChangeMessage.{
  ChangeType => ChangeTypeProto,
  SegmentType => SegmentTypeProto
}
import com.thinkmorestupidless.betfair.proto.streams.MarketDefinition.{
  BettingType => BettingTypeProto,
  MarketStatus => MarketStatusProto
}
import com.thinkmorestupidless.betfair.proto.streams.PriceLadderDefinition.{PriceLadderType => PriceLadderTypeProto}
import com.thinkmorestupidless.betfair.proto.streams.RunnerDefinition.{RunnerStatus => RunnerStatusProto}
import com.thinkmorestupidless.betfair.proto.streams.{
  KeyLineDefinition => KeyLineDefinitionProto,
  KeyLineSelection => KeyLineSelectionProto,
  MarketChange => MarketChangeProto,
  MarketChangeMessage => MarketChangeMessageProto,
  MarketDefinition => MarketDefinitionProto,
  PriceLadderDefinition => PriceLadderDefinitionProto,
  RunnerChange => RunnerChangeProto,
  RunnerDefinition => RunnerDefinitionProto,
  _
}
import com.thinkmorestupidless.betfair.streams.domain._
import com.thinkmorestupidless.grpc.Encoder
import com.thinkmorestupidless.grpc.Encoder._
import com.thinkmorestupidless.betfair.streams.impl.JsonCodecs.LevelBasedPriceLadderOps
import com.thinkmorestupidless.betfair.streams.impl.JsonCodecs.PricePointPriceLadderOps

object Encoders {

  implicit val changeTypeEncoder: Encoder[ChangeType, ChangeTypeProto] =
    _ match {
      case ChangeType.SubImage   => ChangeTypeProto.SUB_IMAGE
      case ChangeType.Heartbeat  => ChangeTypeProto.HEARTBEAT
      case ChangeType.ResubDelta => ChangeTypeProto.RESUB_DELTA
    }

  implicit val segmentTypeEncoder: Encoder[SegmentType, SegmentTypeProto] =
    _ match {
      case SegmentType.Seg      => SegmentTypeProto.SEG
      case SegmentType.SegStart => SegmentTypeProto.SEG_START
      case SegmentType.SegEnd   => SegmentTypeProto.SEG_END
    }

  implicit val marketStatusEncoder: Encoder[MarketStatus, MarketStatusProto] =
    _ match {
      case MarketStatus.Open      => MarketStatusProto.OPEN
      case MarketStatus.Closed    => MarketStatusProto.CLOSED
      case MarketStatus.Inactive  => MarketStatusProto.INACTIVE
      case MarketStatus.Suspended => MarketStatusProto.SUSPENDED
    }

  implicit val bettingTypeEncoder: Encoder[BettingType, BettingTypeProto] =
    _ match {
      case BettingType.Line                    => BettingTypeProto.LINE
      case BettingType.Odds                    => BettingTypeProto.ODDS
      case BettingType.Range                   => BettingTypeProto.RANGE
      case BettingType.AsianHandicapDoubleLine => BettingTypeProto.ASIAN_HANDICAP_DOUBLE_LINE
      case BettingType.AsianHandicapSingleLine => BettingTypeProto.ASIAN_HANDICAP_SINGLE_LINE
    }

  implicit val runnerStatusEncoder: Encoder[RunnerStatus, RunnerStatusProto] =
    _ match {
      case RunnerStatus.Loser         => RunnerStatusProto.LOSER
      case RunnerStatus.Active        => RunnerStatusProto.ACTIVE
      case RunnerStatus.Hidden        => RunnerStatusProto.HIDDEN
      case RunnerStatus.Placed        => RunnerStatusProto.PLACED
      case RunnerStatus.Removed       => RunnerStatusProto.REMOVED
      case RunnerStatus.RemovedVacant => RunnerStatusProto.REMOVED_VACANT
      case RunnerStatus.Winner        => RunnerStatusProto.WINNER
    }

  implicit val priceLadderTypeEncoder: Encoder[PriceLadderType, PriceLadderTypeProto] =
    _ match {
      case PriceLadderType.Finest    => PriceLadderTypeProto.FINEST
      case PriceLadderType.Classic   => PriceLadderTypeProto.CLASSIC
      case PriceLadderType.LineRange => PriceLadderTypeProto.LINE_RANGE
    }

  implicit val arrayOfStringsEncoder: Encoder[List[BigDecimal], ArrayOfStrings] =
    input => ArrayOfStrings(values = input.map(_.toString()))

  implicit val twoDimensionalArrayOfBigDecimalEncoder: Encoder[List[List[BigDecimal]], Seq[ArrayOfStrings]] =
    input => input.map(_.encode)

  implicit val optionalTwoDimensionalArrayOfBigDecimalEncoder
      : Encoder[Option[List[List[BigDecimal]]], Seq[ArrayOfStrings]] =
    input => input.map(_.encode).getOrElse(Seq.empty)

  implicit val levelBasedPriceLadder_seqArrayOfString: Encoder[LevelBasedPriceLadder, Seq[ArrayOfStrings]] =
    _.toOpt.encode

  implicit val pricePointPriceLadder_seqArrayOfString: Encoder[PricePointPriceLadder, Seq[ArrayOfStrings]] =
    _.toOpt.encode

  implicit val runnerChangeEncoder: Encoder[RunnerChange, RunnerChangeProto] =
    runnerChange =>
      RunnerChangeProto(
        tv = runnerChange.tv.toString(),
        batb = runnerChange.batb.encode,
        spb = runnerChange.spb.encode,
        bdatl = runnerChange.bdatl.encode,
        trd = runnerChange.trd.encode,
        spf = runnerChange.spf.map(_.toString()),
        ltp = runnerChange.ltp.map(_.toString()),
        atb = runnerChange.atb.encode,
        spl = runnerChange.spl.encode,
        spn = runnerChange.spn.map(_.toString()),
        atl = runnerChange.atl.encode,
        batl = runnerChange.batl.encode,
        id = runnerChange.id,
        hc = runnerChange.hc.map(_.toString()),
        bdatb = runnerChange.bdatb.encode
      )

  implicit val runnerDefinitionEncoder: Encoder[RunnerDefinition, RunnerDefinitionProto] =
    runnerDefinition =>
      RunnerDefinitionProto(
        sortPriority = runnerDefinition.sortPriority,
        removalDate = runnerDefinition.removalDate,
        id = runnerDefinition.id,
        hc = runnerDefinition.hc.map(_.toString()),
        adjustmentFactor = runnerDefinition.adjustmentFactor.map(_.toString()),
        bsp = runnerDefinition.bsp.map(_.toString()),
        status = runnerDefinition.status.encode
      )

  implicit val priceLadderDefinitionEncoder: Encoder[PriceLadderDefinition, PriceLadderDefinitionProto] =
    priceLadderDefinition => PriceLadderDefinitionProto(`type` = priceLadderDefinition.`type`.encode)

  implicit val keyLineDefinitionEncoder: Encoder[KeyLineDefinition, KeyLineDefinitionProto] =
    keyLineDefinition => KeyLineDefinitionProto.defaultInstance.withKl(keyLineDefinition.kl.encode)

  implicit val keyLineSelectionEncoder: Encoder[KeyLineSelection, KeyLineSelectionProto] =
    keyLineSelection => KeyLineSelectionProto(id = keyLineSelection.id, hc = keyLineSelection.hc.toString())

  implicit val marketDefinitionProto: Encoder[MarketDefinition, MarketDefinitionProto] =
    marketDefinition =>
      MarketDefinitionProto(
        status = marketDefinition.status.encode,
        venue = marketDefinition.venue,
        settledTime = marketDefinition.settledTime,
        timezone = marketDefinition.timezone,
        eachWayDivisor = marketDefinition.eachWayDivisor.map(_.toString()),
        regulators = marketDefinition.regulators,
        marketType = marketDefinition.marketType,
        marketBaseRate = marketDefinition.marketBaseRate.toString(),
        numberOfWinners = marketDefinition.numberOfWinners,
        countryCode = marketDefinition.countryCode,
        lineMaxUnit = marketDefinition.lineMaxUnit.map(_.toString()),
        inPlay = marketDefinition.inPlay,
        betDelay = marketDefinition.betDelay,
        bspMarket = marketDefinition.bspMarket,
        bettingType = marketDefinition.bettingType.encode,
        numberOfActiveRunners = marketDefinition.numberOfActiveRunners,
        lineMinUnit = marketDefinition.lineMinUnit.map(_.toString()),
        eventId = marketDefinition.eventId,
        crossMatching = marketDefinition.crossMatching,
        runnersVoidable = marketDefinition.runnersVoidable,
        turnInPlayEnabled = marketDefinition.turnInPlayEnabled,
        priceLadderDefinition = Some(marketDefinition.priceLadderDefinition.encode),
        keyLineDefinition = marketDefinition.keyLineDefinition.map(_.encode),
        suspendTime = marketDefinition.suspendTime,
        discountAllowed = marketDefinition.discountAllowed,
        persistenceEnabled = marketDefinition.persistenceEnabled,
        runners = marketDefinition.runners.map(_.encode),
        version = marketDefinition.version,
        eventTypeId = marketDefinition.eventTypeId,
        complete = marketDefinition.complete,
        openDate = marketDefinition.openDate,
        marketTime = marketDefinition.marketTime,
        bspReconciled = marketDefinition.bspReconciled
      )

  implicit val marketChangeEncoder: Encoder[MarketChange, MarketChangeProto] =
    marketChange =>
      MarketChangeProto(
        rc = marketChange.rc.map(_.encode),
        img = marketChange.img,
        tv = marketChange.tv.map(_.toString()),
        con = marketChange.con,
        marketDefinition = marketChange.marketDefinition.map(_.encode),
        id = marketChange.id.value
      )

  implicit val marketChangeMessageEncode: Encoder[MarketChangeMessage, MarketChangeMessageProto] =
    marketChangeMessage =>
      MarketChangeMessageProto(
        id = marketChangeMessage.id,
        ct = marketChangeMessage.ct.map(_.encode),
        clk = marketChangeMessage.clk,
        heartbeatMs = marketChangeMessage.heartbeatMs,
        pt = marketChangeMessage.pt,
        initialClk = marketChangeMessage.initialClk,
        mc = marketChangeMessage.mc.map(_.encode).toSeq,
        conflateMs = marketChangeMessage.conflateMs,
        segmentType = marketChangeMessage.segmentType.map(_.encode),
        status = marketChangeMessage.status
      )
}

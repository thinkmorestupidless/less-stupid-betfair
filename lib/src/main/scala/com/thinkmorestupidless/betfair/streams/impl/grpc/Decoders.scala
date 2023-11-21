package com.thinkmorestupidless.betfair.streams.impl.grpc

import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.traverse._
import cats.syntax.validated._
import com.thinkmorestupidless.betfair.proto.streams.MarketDefinition.{
  BettingType => BettingTypeProto,
  MarketStatus => MarketStatusProto
}
import com.thinkmorestupidless.betfair.proto.streams.PriceLadderDefinition.{PriceLadderType => PriceLadderTypeProto}
import com.thinkmorestupidless.betfair.proto.streams.RunnerDefinition.{RunnerStatus => RunnerStatusProto}
import com.thinkmorestupidless.betfair.proto.streams.{
  ArrayOfStrings,
  KeyLineDefinition => KeyLineDefinitionProto,
  KeyLineSelection => KeyLineSelectionProto,
  MarketChange => MarketChangeProto,
  MarketChangeMessage => MarketChangeMessageProto,
  MarketDefinition => MarketDefinitionProto,
  MarketFilter => MarketFilterProto,
  PriceLadderDefinition => PriceLadderDefinitionProto,
  RunnerChange => RunnerChangeProto,
  RunnerDefinition => RunnerDefinitionProto,
  SubscribeToMarketChangesRequest => SubscribeToMarketChangesRequestProto
}
import com.thinkmorestupidless.betfair.streams.domain._
import com.thinkmorestupidless.betfair.streams.impl.grpc.GrpcStreamsServiceImpl.SubscribeToMarketChangesRequest
import com.thinkmorestupidless.grpc.Decoder
import com.thinkmorestupidless.grpc.Decoder._
import com.thinkmorestupidless.grpc.DefaultDecoders._
import com.thinkmorestupidless.utils.Validation.ImplicitConversions.toValidatedOptionalList
import com.thinkmorestupidless.utils.Validation.Validation
import com.thinkmorestupidless.utils.ValidationException
import enumeratum.EnumEntry
import pl.iterators.kebs.macros.enums.EnumOf
import com.thinkmorestupidless.grpc.DefaultDecoders._
import com.thinkmorestupidless.utils.Validation.Validation
import com.thinkmorestupidless.utils.Validation.ImplicitConversions.toValidatedOptionalList
import com.thinkmorestupidless.betfair.proto.streams.MarketFilter.{MarketBettingType => MarketBettingTypeProto}

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object Decoders {

  private def validateEnum[A <: EnumEntry](str: String)(implicit e: EnumOf[A]): Validation[A] =
    Try(e.`enum`.withNameInsensitive(str)) match {
      case Success(result) => result.validNel[ValidationException]
      case Failure(error) =>
        ValidationException(s"'$str' is not a valid member of enum ${e.`enum`.getClass.getSimpleName}", Some(error))
          .invalidNel[A]
    }

  implicit val marketChangeMessageProto_marketChangeMessage: Decoder[MarketChangeMessageProto, MarketChangeMessage] = {
    marketChangeMessageProto =>
      val id: Validation[Option[Int]] = marketChangeMessageProto.id.validNel
      val ct: Validation[Option[ChangeType]] =
        marketChangeMessageProto.ct.fold(validNone[ChangeType])(changeType =>
          validateEnum[ChangeType](changeType.name).map(Some(_))
        )
      val clk: Validation[String] = marketChangeMessageProto.clk.validNel
      val heartbeatMs: Validation[Option[Long]] = marketChangeMessageProto.heartbeatMs.validNel
      val pt: Validation[Long] = marketChangeMessageProto.pt.validNel
      val initialClk: Validation[Option[String]] = marketChangeMessageProto.initialClk.validNel
      val mc: Validation[Option[Set[MarketChange]]] = marketChangeMessageProto.mc.toList match {
        case Nil  => validNone[Set[MarketChange]]
        case list => list.map(_.decode[MarketChange]).sequence.map(list => Some(list.toSet))
      }
      val conflateMs: Validation[Option[Long]] = marketChangeMessageProto.conflateMs.validNel
      val segmentType: Validation[Option[SegmentType]] =
        marketChangeMessageProto.segmentType.fold(validNone[SegmentType])(segmentType =>
          validateEnum[SegmentType](segmentType.name).map(Some(_))
        )
      val status: Validation[Option[Int]] = marketChangeMessageProto.status.validNel

      (id, ct, clk, heartbeatMs, pt, initialClk, mc, conflateMs, segmentType, status).mapN(MarketChangeMessage.apply _)
  }

  implicit val marketChangeProto_marketChange: Decoder[MarketChangeProto, MarketChange] =
    proto => {
      val rc: Validation[Option[List[RunnerChange]]] = proto.rc.toList.map(_.decode).sequence
      val img: Validation[Option[Boolean]] = proto.img.validNel
      val tv: Validation[Option[BigDecimal]] = proto.tv.map(BigDecimal(_)).validNel
      val con: Validation[Option[Boolean]] = proto.con.validNel
      val marketDefinition: Validation[Option[MarketDefinition]] = proto.marketDefinition.map(_.decode).sequence
      val id: Validation[MarketId] = MarketId(proto.id).validNel

      (rc, img, tv, con, marketDefinition, id).mapN(MarketChange.apply _)
    }

  implicit val marketStatusProto_marketStatus: Decoder[MarketStatusProto, MarketStatus] =
    _ match {
      case MarketStatusProto.CLOSED    => MarketStatus.Closed.validNel
      case MarketStatusProto.SUSPENDED => MarketStatus.Suspended.validNel
      case MarketStatusProto.INACTIVE  => MarketStatus.Inactive.validNel
      case MarketStatusProto.OPEN      => MarketStatus.Open.validNel
      case MarketStatusProto.Unrecognized(unrecognizedValue) =>
        ValidationException(s"'$unrecognizedValue' is not a valid member of MarketStatus").invalidNel
    }

  implicit val bettingTypeProto_bettingType: Decoder[BettingTypeProto, BettingType] =
    _ match {
      case BettingTypeProto.ODDS                       => BettingType.Odds.validNel
      case BettingTypeProto.RANGE                      => BettingType.Range.validNel
      case BettingTypeProto.LINE                       => BettingType.Line.validNel
      case BettingTypeProto.ASIAN_HANDICAP_SINGLE_LINE => BettingType.AsianHandicapSingleLine.validNel
      case BettingTypeProto.ASIAN_HANDICAP_DOUBLE_LINE => BettingType.AsianHandicapDoubleLine.validNel
      case BettingTypeProto.Unrecognized(unrecognizedValue) =>
        ValidationException(s"'$unrecognizedValue' is not a valid member of BettingType").invalidNel
    }

  implicit val marketBettingTypeProto_marketBettingType: Decoder[MarketBettingTypeProto, MarketBettingType] =
    _ match {
      case MarketBettingTypeProto.LINE                       => MarketBettingType.Line.validNel
      case MarketBettingTypeProto.ODDS                       => MarketBettingType.Odds.validNel
      case MarketBettingTypeProto.RANGE                      => MarketBettingType.Range.validNel
      case MarketBettingTypeProto.FIXED_ODDS                 => MarketBettingType.FixedOdds.validNel
      case MarketBettingTypeProto.ASIAN_HANDICAP_DOUBLE_LINE => MarketBettingType.AsianHandicapDoubleLine.validNel
      case MarketBettingTypeProto.ASIAN_HANDICAP_SINGLE_LINE => MarketBettingType.AsianHandicapSingleLine.validNel
      case MarketBettingTypeProto.Unrecognized(unrecognizedValue) =>
        ValidationException(s"'$unrecognizedValue' is not a valid member of MarketBettingType").invalidNel
    }

  implicit val priceLadderTypeProto_priceLadderType: Decoder[PriceLadderTypeProto, PriceLadderType] =
    _ match {
      case PriceLadderTypeProto.CLASSIC    => PriceLadderType.Classic.validNel
      case PriceLadderTypeProto.LINE_RANGE => PriceLadderType.LineRange.validNel
      case PriceLadderTypeProto.FINEST     => PriceLadderType.Finest.validNel
      case PriceLadderTypeProto.Unrecognized(unrecognizedValue) =>
        ValidationException(s"'$unrecognizedValue' is not a valid member of PriceLadderType").invalidNel
    }

  implicit val runnerStatusProto_runnerStatus: Decoder[RunnerStatusProto, RunnerStatus] =
    _ match {
      case RunnerStatusProto.WINNER         => RunnerStatus.Winner.validNel
      case RunnerStatusProto.REMOVED_VACANT => RunnerStatus.RemovedVacant.validNel
      case RunnerStatusProto.REMOVED        => RunnerStatus.Removed.validNel
      case RunnerStatusProto.PLACED         => RunnerStatus.Placed.validNel
      case RunnerStatusProto.HIDDEN         => RunnerStatus.Hidden.validNel
      case RunnerStatusProto.ACTIVE         => RunnerStatus.Active.validNel
      case RunnerStatusProto.LOSER          => RunnerStatus.Loser.validNel
      case RunnerStatusProto.Unrecognized(unrecognizedValue) =>
        ValidationException(s"'$unrecognizedValue' is not a valid member of RunnerStatus").invalidNel
    }

  implicit val priceLadderDefinitionProto_priceLadderDefinition
      : Decoder[PriceLadderDefinitionProto, PriceLadderDefinition] =
    proto => proto.`type`.decode.map(PriceLadderDefinition(_))

  implicit val keyLineSelectionProto_keyLineSelection: Decoder[KeyLineSelectionProto, KeyLineSelection] =
    proto => {
      val id = proto.id.validNel
      val hc = BigDecimal(proto.hc).validNel

      (id, hc).mapN(KeyLineSelection.apply _)
    }

  implicit val keyLineDefinitionProto_keyLineDefinition: Decoder[KeyLineDefinitionProto, KeyLineDefinition] =
    proto =>
      ensureOptionIsDefined(proto.kl, "KeyLineDefinition.kl is a required field")
        .andThen(_.decode)
        .map(KeyLineDefinition(_))

  implicit val runnerDefinitionProto_runnerDefinition: Decoder[RunnerDefinitionProto, RunnerDefinition] =
    proto => {
      val sortPriority = proto.sortPriority.validNel
      val removalDate = proto.removalDate.validNel
      val id = proto.id.validNel
      val hc = proto.hc.map(BigDecimal(_)).validNel
      val adjustmentFactor = proto.adjustmentFactor.map(BigDecimal(_)).validNel
      val bsp = proto.bsp.map(BigDecimal(_)).validNel
      val status = proto.status.decode

      (sortPriority, removalDate, id, hc, adjustmentFactor, bsp, status).mapN(RunnerDefinition.apply _)
    }

  implicit val optionalPriceLadderDefinition_priceLadderDefinition
      : Decoder[Option[PriceLadderDefinitionProto], PriceLadderDefinition] =
    proto =>
      ensureOptionIsDefined(
        proto,
        "MarketDefinition.priceLadderDefinition is a required field"
      ).andThen(_.decode)

  implicit val optionalKeyLineDefinition_keyLineDefinition
      : Decoder[Option[KeyLineDefinitionProto], Option[KeyLineDefinition]] =
    _.map(_.decode).sequence

  implicit val marketDefinitionProto_marketDefinition: Decoder[MarketDefinitionProto, MarketDefinition] =
    proto =>
      (for {
        status <- proto.status.decode.toEither
        venue <- proto.venue.decode[Option[String]].toEither
        settledTime <- proto.settledTime.decode[Option[String]].toEither
        timezone <- proto.timezone.decode[String].toEither
        eachWayDivisor <- proto.eachWayDivisor.decode[Option[BigDecimal]].toEither
        regulators <- proto.regulators.decode.toEither
        marketType <- proto.marketType.decode[String].toEither
        marketBaseRate <- proto.marketBaseRate.decode[BigDecimal].toEither
        numberOfWinners <- proto.numberOfWinners.decode.toEither
        countryCode <- proto.countryCode.decode[String].toEither
        lineMaxUnit <- proto.lineMaxUnit.decode[Option[BigDecimal]].toEither
        inPlay <- proto.inPlay.decode.toEither
        betDelay <- proto.betDelay.decode.toEither
        bspMarket <- proto.bspMarket.decode.toEither
        bettingType <- proto.bettingType.decode.toEither
        numberOfActiveRunners <- proto.numberOfActiveRunners.decode.toEither
        lineMinUnit <- proto.lineMinUnit.decode[Option[BigDecimal]].toEither
        eventId <- proto.eventId.decode[String].toEither
        crossMatching <- proto.crossMatching.decode.toEither
        runnersVoidable <- proto.runnersVoidable.decode.toEither
        turnInPlayEnabled <- proto.turnInPlayEnabled.decode.toEither
        priceLadderDefinition <- proto.priceLadderDefinition
          .decode(optionalPriceLadderDefinition_priceLadderDefinition)
          .toEither
        keyLineDefinition <- proto.keyLineDefinition.decode.toEither
        suspendTime <- proto.suspendTime.decode[String].toEither
        discountAllowed <- proto.discountAllowed.decode.toEither
        persistenceEnabled <- proto.persistenceEnabled.decode.toEither
        runners <- proto.runners.toList.map(_.decode).sequence.toEither
        version <- proto.version.decode.toEither
        eventTypeId <- proto.eventTypeId.decode[String].toEither
        complete <- proto.complete.decode.toEither
        openDate <- proto.openDate.decode[String].toEither
        marketTime <- proto.marketTime.decode[String].toEither
        bspReconciled <- proto.bspReconciled.decode.toEither
        lineInterval <- proto.lineInterval.decode[Option[BigDecimal]].toEither
      } yield MarketDefinition(
        status,
        venue,
        settledTime,
        timezone,
        eachWayDivisor,
        regulators,
        marketType,
        marketBaseRate,
        numberOfWinners,
        countryCode,
        lineMaxUnit,
        inPlay,
        betDelay,
        bspMarket,
        bettingType,
        numberOfActiveRunners,
        lineMinUnit,
        eventId,
        crossMatching,
        runnersVoidable,
        turnInPlayEnabled,
        priceLadderDefinition,
        keyLineDefinition,
        suspendTime,
        discountAllowed,
        persistenceEnabled,
        runners,
        version,
        eventTypeId,
        complete,
        openDate,
        marketTime,
        bspReconciled,
        lineInterval
      )).toValidated

  implicit val arrayOfStrings_listBigDecimal: Decoder[ArrayOfStrings, List[BigDecimal]] =
    _.values.map(BigDecimal(_)).toList.validNel

  implicit val seqArrayOfStrings_optionalListListBigDecimal
      : Decoder[Seq[ArrayOfStrings], Option[List[List[BigDecimal]]]] =
    _.toList
      .map(_.decode)
      .sequence
      .map(_ match {
        case Nil  => None
        case list => Some(list)
      })

  implicit val runnerChangeProto_runnerChange: Decoder[RunnerChangeProto, RunnerChange] =
    proto => {
      val tv = proto.tv.map(BigDecimal(_)).validNel
      val batb = proto.batb.decode
      val spb: Validation[Option[List[List[BigDecimal]]]] = proto.spb.decode
      val bdatl: Validation[Option[List[List[BigDecimal]]]] = proto.bdatl.decode
      val trd: Validation[Option[List[List[BigDecimal]]]] = proto.trd.decode
      val spf: Validation[Option[BigDecimal]] = proto.spf.map(BigDecimal(_)).validNel
      val ltp: Validation[Option[BigDecimal]] = proto.ltp.map(BigDecimal(_)).validNel
      val atb: Validation[Option[List[List[BigDecimal]]]] = proto.atb.decode
      val spl: Validation[Option[List[List[BigDecimal]]]] = proto.spl.decode
      val spn: Validation[Option[BigDecimal]] = proto.spn.map(BigDecimal(_)).validNel
      val atl: Validation[Option[List[List[BigDecimal]]]] = proto.atl.decode
      val batl: Validation[Option[List[List[BigDecimal]]]] = proto.batl.decode
      val id: Validation[Long] = proto.id.validNel
      val hc: Validation[Option[BigDecimal]] = proto.hc.map(BigDecimal(_)).validNel
      val bdatb: Validation[Option[List[List[BigDecimal]]]] = proto.bdatb.decode

      (tv, batb, spb, bdatl, trd, spf, ltp, atb, spl, spn, atl, batl, id, hc, bdatb).mapN(RunnerChange.apply _)
    }

  implicit val subscribeToMarketChangesRequestProto_subscribeToMarketChangeRequest
      : Decoder[SubscribeToMarketChangesRequestProto, SubscribeToMarketChangesRequest] =
    proto =>
      ensureOptionIsDefined(
        proto.marketFilter,
        "required field SubscribeToMarketChangesRequest.marketFilter was not defined"
      ).andThen(_.decode[MarketFilter]).map(SubscribeToMarketChangesRequest(_))

  implicit val string_marketId: Decoder[String, MarketId] = MarketId(_).validNel

  implicit val marketFilterProto_marketFilter: Decoder[MarketFilterProto, MarketFilter] =
    proto => {
      val marketIds = proto.marketIds.decode[Option[List[MarketId]]]
      val bspMarket = proto.bspMarket.decode[Option[Boolean]]
      val bettingTypes = proto.bettingTypes.decode[Option[List[MarketBettingType]]]
      val eventTypeIds = proto.eventTypeIds.decode[Option[List[String]]]
      val eventIds = proto.eventIds.decode[Option[List[String]]]
      val turnInPlayEnabled = proto.turnInPlayEnabled.decode[Option[Boolean]]
      val marketTypes = proto.marketTypes.decode[Option[List[String]]]
      val venues = proto.venues.decode[Option[List[String]]]
      val countryCodes = proto.countryCodes.decode[Option[List[String]]]
      val raceTypes = proto.raceTypes.decode[Option[List[String]]]

      (
        marketIds,
        bspMarket,
        bettingTypes,
        eventTypeIds,
        eventIds,
        turnInPlayEnabled,
        marketTypes,
        venues,
        countryCodes,
        raceTypes
      ).mapN(
        (
            marketIds,
            bspMarket,
            bettingTypes,
            eventTypeIds,
            eventIds,
            turnInPlayEnabled,
            marketTypes,
            venues,
            countryCodes,
            raceTypes
        ) =>
          MarketFilter(
            marketIds,
            bspMarket,
            bettingTypes,
            eventTypeIds,
            eventIds,
            turnInPlayEnabled,
            marketTypes,
            venues,
            countryCodes,
            raceTypes
          )
      )
    }
}

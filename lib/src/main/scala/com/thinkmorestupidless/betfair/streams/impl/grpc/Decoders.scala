package com.thinkmorestupidless.betfair.streams.impl.grpc

import cats.syntax.apply._
import cats.syntax.traverse._
import cats.syntax.validated._
import com.thinkmorestupidless.betfair.proto.streams.{MarketChange => MarketChangeProto}
import com.thinkmorestupidless.betfair.proto.streams.{MarketChangeMessage => MarketChangeMessageProto}
import com.thinkmorestupidless.betfair.streams.domain.{ChangeType, MarketChange, MarketChangeMessage, Op, SegmentType}
import com.thinkmorestupidless.grpc.Decoder
import com.thinkmorestupidless.grpc.Decoder._
import com.thinkmorestupidless.utils.Validation.Validation
import com.thinkmorestupidless.utils.ValidationException
import enumeratum.{Enum, EnumEntry}
import pl.iterators.kebs.macros.enums.EnumOf

import scala.util.{Failure, Success, Try}

object Decoders {

  private def validNone[T]: Validation[Option[T]] = None.validNel

  private def validateEnum[A <: EnumEntry](str: String)(implicit e: EnumOf[A]): Validation[A] =
    Try(e.`enum`.withNameInsensitive(str)) match {
      case Success(result) => result.validNel[ValidationException]
      case Failure(error) =>
        ValidationException(s"'$str' is not a valid member of enum ${e.`enum`.getClass.getSimpleName}", Some(error))
          .invalidNel[A]
    }

  implicit val marketChangeMessageDecoder: Decoder[MarketChangeMessageProto, MarketChangeMessage] = {
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

  implicit val decodeMarketChangeDecoder: Decoder[MarketChangeProto, MarketChange] = ???
}

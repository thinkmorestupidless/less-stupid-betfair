package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, SessionToken}
import com.thinkmorestupidless.betfair.auth.impl.JsonCodecs.{applicationKeyCodec, sessionTokenCodec}
import com.thinkmorestupidless.betfair.core.domain.{Money, Price}
import com.thinkmorestupidless.betfair.streams.domain._
import com.thinkmorestupidless.extensions.circe.CirceUtils._
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.slf4j.LoggerFactory

object JsonCodecs {

  private val log = LoggerFactory.getLogger(getClass)

  implicit class ListOps[T](self: List[T]) {
    def toOpt: Option[List[T]] =
      self match {
        case Nil  => None
        case list => Some(list)
      }
  }

  implicit class SetOps[T](self: Set[T]) {
    def toOpt: Option[Set[T]] =
      self.size match {
        case 0 => None
        case _ => Some(self)
      }
  }

  implicit class ResultOptionListListBigDecimalOps(self: Option[List[List[BigDecimal]]]) {
    def toLevelBasedPriceLadder(): LevelBasedPriceLadder =
      self
        .map(_.foldLeft(LevelBasedPriceLadder.empty) { (priceLadder, next) =>
          val level = PriceLadderLevel(next(0).intValue)
          val price = next(1)
          val tradedVolume = next(2)
          if (price == 0 && tradedVolume == 0)
            priceLadder.without(level)
          else {
            val entry = LevelBasedPriceLadderEntry(Price(price), Money(tradedVolume), level)
            priceLadder.add(entry)
          }
        })
        .getOrElse(LevelBasedPriceLadder.empty)

    def toPricePointPriceLadder(): PricePointPriceLadder =
      self
        .map(_.foldLeft(PricePointPriceLadder.empty) { (priceLadder, next) =>
          val price = Price(next(0))
          val tradedVolume = Money(next(1))
          val entry = PricePointPriceLadderEntry(price, tradedVolume)
          priceLadder.add(entry)
        })
        .getOrElse(PricePointPriceLadder.empty)
  }

  implicit class LevelBasedPriceLadderOps(self: LevelBasedPriceLadder) {
    def toOpt: Option[List[List[BigDecimal]]] =
      self.entries match {
        case Nil     => None
        case entries => Some(entries.map(entry => List(entry.price.value, entry.tradedVolume.amount)))
      }
  }

  implicit class PricePointPriceLadderOps(self: PricePointPriceLadder) {
    def toOpt: Option[List[List[BigDecimal]]] =
      self.entries match {
        case Nil => None
        case entries =>
          Some(entries.zipWithIndex.map { case (entry, index) =>
            List(index, entry.price.value, entry.tradedVolume.amount)
          })
      }
  }

  implicit val marketIdCoded: Codec[MarketId] = bimapString(_.value, MarketId(_))
  implicit val marketFilterCodec: Codec[MarketFilter] = deriveCodec[MarketFilter]
  implicit val marketDefinitionCodec: Codec[MarketDefinition] = deriveCodec[MarketDefinition]
  implicit val priceLadderDefinitionCodec: Codec[PriceLadderDefinition] = deriveCodec[PriceLadderDefinition]
  implicit val keyLineDefinitionCodec: Codec[KeyLineDefinition] = deriveCodec[KeyLineDefinition]
  implicit val keyLineSelectionCodec: Codec[KeyLineSelection] = deriveCodec[KeyLineSelection]
  implicit val runnerDefinitionCodec: Codec[RunnerDefinition] = deriveCodec[RunnerDefinition]
  implicit val errorMessageCodec: Codec[ErrorMessage] = bimapString(_.value, ErrorMessage(_))
  implicit val connectionClosedCodec: Codec[ConnectionClosed] = bimapBoolean(_.value, ConnectionClosed(_))
  implicit val connectionIdCodec: Codec[ConnectionId] = bimapString(_.value, ConnectionId(_))
  implicit val socketReadyCodec: Codec[SocketReady.type] = deriveCodec[SocketReady.type]
  implicit val socketFailedCodec: Codec[SocketFailed.type] = deriveCodec[SocketFailed.type]

  implicit val authenticationCodec: Codec[Authentication] = Codec.from(
    cursor =>
      for {
        appKey <- cursor.downField("appKey").as[ApplicationKey]
        session <- cursor.downField("session").as[SessionToken]
      } yield Authentication(appKey, session),
    authentication =>
      Json.obj(
        ("op", authentication.op.asJson),
        ("appKey", Json.fromString(authentication.appKey.value)),
        ("session", Json.fromString(authentication.session.value))
      )
  )

  implicit val connectionCodec: Codec[Connection] = Codec.from(
    _.downField("connectionId").as[ConnectionId].map(Connection(_)),
    connection => Json.obj(("op", connection.op.asJson), ("connectionId", connection.connectionId.asJson))
  )

  implicit val heartbeatCodec: Codec[Heartbeat] = Codec.from(
    _ => Right(Heartbeat()),
    heartbeat => Json.obj(("op", heartbeat.op.asJson))
  )

  implicit val marketSubscriptionCodec: Codec[MarketSubscription] = Codec.from(
    cursor =>
      for {
        marketFilter <- cursor.downField("marketFilter").as[MarketFilter]
      } yield MarketSubscription(marketFilter),
    marketSubscription =>
      Json.obj(
        ("op", marketSubscription.op.asJson),
        ("marketFilter", marketSubscription.marketFilter.asJson)
      )
  )

  implicit val marketChangeCodec: Codec[MarketChange] = Codec.from(
    cursor =>
      for {
        rc <- cursor.downField("rc").as[Option[List[RunnerChange]]]
        img <- cursor.downField("img").as[Option[Boolean]]
        tv <- cursor.downField("tv").as[Option[BigDecimal]]
        con <- cursor.downField("con").as[Option[Boolean]]
        marketDefinition <- cursor.downField("marketDefinition").as[Option[MarketDefinition]]
        id <- cursor.downField("id").as[MarketId]
      } yield MarketChange(rc.getOrElse(List.empty), img, tv, con, marketDefinition, id),
    marketChange =>
      Json.obj(
        ("rc", marketChange.rc.toOpt.asJson),
        ("img", marketChange.img.asJson),
        ("tv", marketChange.tv.asJson),
        ("con", marketChange.con.asJson),
        ("marketDefinition", marketChange.marketDefinition.asJson),
        ("id", marketChange.id.asJson)
      )
  )

  implicit val marketChangeMessageCodec: Codec[MarketChangeMessage] = Codec.from(
    cursor =>
      for {
        id <- cursor.downField("id").as[Option[Int]]
        ct <- cursor.downField("ct").as[Option[ChangeType]]
        clk <- cursor.downField("clk").as[String]
        heartbeatMs <- cursor.downField("heartbeatMs").as[Option[Long]]
        pt <- cursor.downField("pt").as[Long]
        initialClk <- cursor.downField("initialClk").as[Option[String]]
        mc <- cursor.downField("mc").as[Option[Set[MarketChange]]]
        conflateMs <- cursor.downField("conflateMs").as[Option[Long]]
        segmentType <- cursor.downField("segmentType").as[Option[SegmentType]]
        status <- cursor.downField("status").as[Option[Int]]
      } yield MarketChangeMessage(
        id,
        ct,
        clk,
        heartbeatMs,
        pt,
        initialClk,
        mc.getOrElse(Set.empty),
        conflateMs,
        segmentType,
        status
      ),
    marketChangeMessage =>
      Json.obj(
        ("op", marketChangeMessage.op.asJson),
        ("id", marketChangeMessage.id.asJson),
        ("ct", marketChangeMessage.ct.asJson),
        ("clk", marketChangeMessage.clk.asJson),
        ("heartbeatMs", marketChangeMessage.heartbeatMs.asJson),
        ("pt", marketChangeMessage.pt.asJson),
        ("initialClk", marketChangeMessage.initialClk.asJson),
        ("mc", marketChangeMessage.mc.toOpt.asJson),
        ("conflateMs", marketChangeMessage.conflateMs.asJson),
        ("segmentType", marketChangeMessage.segmentType.asJson),
        ("status", marketChangeMessage.status.asJson)
      )
  )

  implicit val pricePointPriceLadderCodec: Codec[PricePointPriceLadder] = Codec.from(
    _.as[Option[List[List[BigDecimal]]]].map(_.toPricePointPriceLadder()),
    _.toOpt.asJson
  )

  implicit val levelBasedPriceLadderCodec: Codec[LevelBasedPriceLadder] = Codec.from(
    _.as[Option[List[List[BigDecimal]]]].map(_.toLevelBasedPriceLadder()),
    _.toOpt.asJson
  )

  implicit class HCursorOps(self: HCursor) {
    def asOrElse[T: Decoder](key: String, default: T): Decoder.Result[T] =
      if (self.keys.map(_.toList.contains(key)).getOrElse(false)) {
        self.downField(key).as[T]
      } else {
        Right(default)
      }
  }

  implicit val runnerChangeCodec: Codec[RunnerChange] = Codec.from(
    cursor =>
      for {
        tradedVolume <- cursor.downField("tv").as[Option[BigDecimal]]
        bestAvailableToBack <- cursor.asOrElse("batb", LevelBasedPriceLadder.empty)
        startingPriceAvailableToBack <- cursor.downField("spb").as[Option[List[List[BigDecimal]]]]
        bestDisplayAvailableToLay <- cursor.asOrElse("bdatl", LevelBasedPriceLadder.empty)
        traded <- cursor.asOrElse("trd", PricePointPriceLadder.empty)
        spfJson <- cursor.downField("spf").as[Json]
        lastTradedPrice <- cursor.downField("ltp").as[Option[BigDecimal]]
        availableToBack <- cursor.asOrElse("atb", PricePointPriceLadder.empty)
        startingPriceAvailableToLay <- cursor.downField("spl").as[Option[List[List[BigDecimal]]]]
        startingPriceNear <- cursor.downField("spn").as[Option[BigDecimal]]
        availableToLay <- cursor.asOrElse("atl", PricePointPriceLadder.empty)
        bestAvailableToLay <- cursor.asOrElse("batl", LevelBasedPriceLadder.empty)
        id <- cursor.downField("id").as[Long]
        hc <- cursor.downField("hc").as[Option[BigDecimal]]
        bestDisplayAvailableToBack <- cursor.asOrElse("bdatb", LevelBasedPriceLadder.empty)
      } yield {
        val startingPriceFar = if (spfJson.isString) {
          None
        } else if (spfJson.isNumber) {
          spfJson.asNumber.flatMap(_.toBigDecimal)
        } else {
          None
        }
        RunnerChange(
          tradedVolume,
          bestAvailableToBack,
          startingPriceAvailableToBack.getOrElse(List.empty),
          bestDisplayAvailableToLay,
          traded,
          startingPriceFar,
          lastTradedPrice,
          availableToBack,
          startingPriceAvailableToLay.getOrElse(List.empty),
          startingPriceNear,
          availableToLay,
          bestAvailableToLay,
          id,
          hc,
          bestDisplayAvailableToBack
        )
      },
    runnerChange =>
      Json.obj(
        ("tv", runnerChange.tv.asJson),
        ("batb", runnerChange.batb.toOpt.asJson),
        ("spb", runnerChange.spb.toOpt.asJson),
        ("bdatl", runnerChange.bdatl.toOpt.asJson),
        ("trd", runnerChange.trd.toOpt.asJson),
        ("spf", runnerChange.spf.asJson),
        ("ltp", runnerChange.ltp.asJson),
        ("atb", runnerChange.atb.toOpt.asJson),
        ("spl", runnerChange.spl.toOpt.asJson),
        ("spn", runnerChange.spn.asJson),
        ("atl", runnerChange.atl.toOpt.asJson),
        ("batl", runnerChange.batl.toOpt.asJson),
        ("id", runnerChange.id.asJson),
        ("hc", runnerChange.hc.asJson),
        ("bdatb", runnerChange.bdatb.toOpt.asJson)
      )
  )

  implicit val incomingMessageCodec: Codec[IncomingBetfairSocketMessage] = Codec.from(
    new Decoder[IncomingBetfairSocketMessage] {
      override def apply(cursor: HCursor): Result[IncomingBetfairSocketMessage] =
        cursor.downField("op").as[Op] match {
          case Right(op)   => decodeOp(op, cursor)
          case Left(error) => throw new IllegalStateException(s"failed to decode incoming message - because [$error]")
        }

      def decodeOp(op: Op, cursor: HCursor): Result[IncomingBetfairSocketMessage] =
        op match {
          case Op.Connection => cursor.value.as[Connection]
          case Op.mcm        => cursor.value.as[MarketChangeMessage]
          case Op.Status     => decodeStatus(cursor)
          case _             => throw new IllegalStateException(s"expected an incoming op but received '$op'")
        }

      def decodeStatus(cursor: HCursor): Result[IncomingBetfairSocketMessage] = {
        val statusCode = cursor.downField("statusCode").as[StatusCode] match {
          case Right(status) => status
          case Left(error) =>
            throw new IllegalStateException(
              s"failed to decode status code - expected 'op' to be either 'SUCCESS' or 'FAILURE' but [$error]"
            )
        }
        statusCode match {
          case StatusCode.Success => cursor.downField("connectionClosed").as[ConnectionClosed].map(Success(_))
          case StatusCode.Failure =>
            for {
              errorCode <- cursor.downField("errorCode").as[ErrorCode]
              errorMessage <- cursor.downField("errorMessage").as[ErrorMessage]
              connectionClosed <- cursor.downField("connectionClosed").as[ConnectionClosed]
              connectionId <- cursor.downField("connectionId").as[ConnectionId]
            } yield Failure(errorCode, errorMessage, connectionClosed, connectionId)
        }
      }
    },
    _ match {
      case connection: Connection                   => connection.asJson
      case SocketFailed                             => SocketFailed.asJson
      case SocketReady                              => SocketReady.asJson
      case marketChangeMessage: MarketChangeMessage => marketChangeMessage.asJson
      case Success(connectionClosed) =>
        Json.obj(
          ("op", Json.fromString("status")),
          ("statusCode", Json.fromString("SUCCESS")),
          ("connectionClosed", connectionClosed.asJson)
        )
      case Failure(errorCode, errorMessage, connectionClosed, connectionId) =>
        Json.obj(
          ("op", Json.fromString("status")),
          ("statusCode", Json.fromString("FAILURE")),
          ("errorCode", errorCode.asJson),
          ("errorMessage", errorMessage.asJson),
          ("connectionClosed", connectionClosed.asJson),
          ("connectionId", connectionId.asJson)
        )
      case x => throw new NotImplementedError(s"no encoder for $x")
    }
  )

  implicit val outgoingMessageCodec: Codec[OutgoingBetfairSocketMessage] = Codec.from(
    new Decoder[OutgoingBetfairSocketMessage] {
      override def apply(cursor: HCursor): Result[OutgoingBetfairSocketMessage] =
        cursor.downField("op").as[Op] match {
          case Right(op) => decodeOp(op, cursor)
          case Left(error) =>
            log.warn(s"failed to decode outgoing message op - because [$error]")
            throw new IllegalStateException(s"failed to decode outgoing message op - because [$error]")
        }

      def decodeOp(op: Op, c: HCursor): Result[OutgoingBetfairSocketMessage] =
        op match {
          case Op.Authentication     => c.as[Authentication]
          case Op.Heartbeat          => c.as[Heartbeat]
          case Op.MarketSubscription => c.as[MarketSubscription]
          case _                     => throw new IllegalStateException(s"expected an outgoing op but received '$op'")
        }
    },
    _ match {
      case marketSubscription: MarketSubscription => marketSubscription.asJson
      case heartbeat: Heartbeat                   => heartbeat.asJson
      case authentication: Authentication         => authentication.asJson
    }
  )
}

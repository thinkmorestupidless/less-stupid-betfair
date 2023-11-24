package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, SessionToken}
import com.thinkmorestupidless.betfair.auth.impl.JsonCodecs.{applicationKeyCodec, sessionTokenCodec}
import com.thinkmorestupidless.betfair.streams.domain._
import com.thinkmorestupidless.extensions.circe.CirceUtils._
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.slf4j.LoggerFactory

object JsonCodecs {

  private val log = LoggerFactory.getLogger(getClass)

  implicit val marketIdCoded: Codec[MarketId] = bimapString(_.value, MarketId(_))

  implicit val marketFilterCodec: Codec[MarketFilter] = deriveCodec[MarketFilter]
  implicit val marketChangeCodec: Codec[MarketChange] = deriveCodec[MarketChange]
//  implicit val runnerChangeCodec: Codec[RunnerChange] = deriveCodec[RunnerChange]
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
        mc,
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
        ("mc", marketChangeMessage.mc.asJson),
        ("conflateMs", marketChangeMessage.conflateMs.asJson),
        ("segmentType", marketChangeMessage.segmentType.asJson),
        ("status", marketChangeMessage.status.asJson)
      )
  )

  implicit val runnerChangeCodec: Codec[RunnerChange] = Codec.from(
    new Decoder[RunnerChange] {
      override def apply(cursor: HCursor): Result[RunnerChange] =
        for {
          tv <- cursor.downField("tv").as[Option[BigDecimal]]
          batb <- cursor.downField("batb").as[Option[List[List[BigDecimal]]]]
          spb <- cursor.downField("spb").as[Option[List[List[BigDecimal]]]]
          bdatl <- cursor.downField("bdatl").as[Option[List[List[BigDecimal]]]]
          trd <- cursor.downField("trd").as[Option[List[List[BigDecimal]]]]
          spfJson <- cursor.downField("spf").as[Json]
          ltp <- cursor.downField("ltp").as[Option[BigDecimal]]
          atb <- cursor.downField("atb").as[Option[List[List[BigDecimal]]]]
          spl <- cursor.downField("spl").as[Option[List[List[BigDecimal]]]]
          spn <- cursor.downField("spn").as[Option[BigDecimal]]
          atl <- cursor.downField("atl").as[Option[List[List[BigDecimal]]]]
          batl <- cursor.downField("batl").as[Option[List[List[BigDecimal]]]]
          id <- cursor.downField("id").as[Long]
          hc <- cursor.downField("hc").as[Option[BigDecimal]]
          bdatb <- cursor.downField("bdatb").as[Option[List[List[BigDecimal]]]]
        } yield {
          val spf = if (spfJson.isString) {
            None
          } else if (spfJson.isNumber) {
            spfJson.asNumber.flatMap(_.toBigDecimal)
          } else {
            None
          }
          RunnerChange(tv, batb, spb, bdatl, trd, spf, ltp, atb, spl, spn, atl, batl, id, hc, bdatb)
        }
    },
    new Encoder[RunnerChange] {
      override def apply(runnerChange: RunnerChange): Json =
        Json.obj(
          ("tv", runnerChange.tv.asJson),
          ("batb", runnerChange.batb.asJson),
          ("spb", runnerChange.spb.asJson),
          ("bdatl", runnerChange.bdatl.asJson),
          ("trd", runnerChange.trd.asJson),
          ("spf", runnerChange.spf.asJson),
          ("ltp", runnerChange.ltp.asJson),
          ("atb", runnerChange.atb.asJson),
          ("spl", runnerChange.spl.asJson),
          ("spn", runnerChange.spn.asJson),
          ("atl", runnerChange.atl.asJson),
          ("batl", runnerChange.batl.asJson),
          ("id", runnerChange.id.asJson),
          ("hc", runnerChange.hc.asJson),
          ("bdatb", runnerChange.bdatb.asJson)
        )
    }
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

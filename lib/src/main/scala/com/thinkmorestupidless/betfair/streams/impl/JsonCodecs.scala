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
  implicit val runnerChangeCodec: Codec[RunnerChange] = deriveCodec[RunnerChange]
  implicit val marketDefinitionCodec: Codec[MarketDefinition] = deriveCodec[MarketDefinition]
  implicit val priceLadderDefinitionCodec: Codec[PriceLadderDefinition] = deriveCodec[PriceLadderDefinition]
  implicit val keyLineDefinitionCodec: Codec[KeyLineDefinition] = deriveCodec[KeyLineDefinition]
  implicit val keyLineSelectionCodec: Codec[KeyLineSelection] = deriveCodec[KeyLineSelection]
  implicit val runnerDefinitionCodec: Codec[RunnerDefinition] = deriveCodec[RunnerDefinition]

  implicit val errorMessageCodec: Codec[ErrorMessage] = bimapString(_.value, ErrorMessage(_))
  implicit val connectionClosedCodec: Codec[ConnectionClosed] = bimapBoolean(_.value, ConnectionClosed(_))
  implicit val connectionIdCodec: Codec[ConnectionId] = bimapString(_.value, ConnectionId(_))

  implicit val heartbeatCodec: Codec[Heartbeat.type] = deriveCodec[Heartbeat.type]
  implicit val socketReadyCodec: Codec[SocketReady.type] = deriveCodec[SocketReady.type]
  implicit val socketFailedCodec: Codec[SocketFailed.type] = deriveCodec[SocketFailed.type]

  implicit val authenticationCodec: Codec[Authentication] = Codec.from(
    new Decoder[Authentication] {
      override def apply(c: HCursor): Result[Authentication] =
        for {
          appKey <- c.downField("appKey").as[ApplicationKey]
          session <- c.downField("session").as[SessionToken]
        } yield Authentication(appKey, session)
    },
    new Encoder[Authentication] {
      override def apply(a: Authentication): Json = {
        log.info(s"Encoding authentication [${a.op.asJson}]")
        Json.obj(
          ("op", a.op.asJson),
          ("appKey", Json.fromString(a.appKey.value)),
          ("session", Json.fromString(a.session.value))
        )
      }
    }
  )
  implicit val connectionCodec: Codec[Connection] = Codec.from(
    new Decoder[Connection] {
      override def apply(c: HCursor): Result[Connection] =
        c.downField("connectionId").as[ConnectionId].map(Connection(_))
    },
    new Encoder[Connection] {
      override def apply(connection: Connection): Json =
        Json.obj(("op", Json.fromString("connection")), ("connectionId", connection.connectionId.asJson))
    }
  )

  implicit val marketSubscriptionCodec: Codec[MarketSubscription] = Codec.from(
    new Decoder[MarketSubscription] {
      override def apply(c: HCursor): Result[MarketSubscription] =
        for {
          marketFilter <- c.downField("marketFilter").as[MarketFilter]
        } yield MarketSubscription(marketFilter)
    },
    new Encoder[MarketSubscription] {
      override def apply(a: MarketSubscription): Json =
        Json.obj(
          ("op", Json.fromString("marketSubscription")),
          ("marketFilter", a.marketFilter.asJson)
        )
    }
  )

  implicit val marketChangeMessageCodec: Codec[MarketChangeMessage] = Codec.from(
    new Decoder[MarketChangeMessage] {
      override def apply(c: HCursor): Result[MarketChangeMessage] =
        for {
          id <- c.downField("id").as[Option[Int]]
          ct <- c.downField("ct").as[Option[ChangeType]]
          clk <- c.downField("clk").as[String]
          heartbeatMs <- c.downField("heartbeatMs").as[Option[Long]]
          pt <- c.downField("pt").as[Long]
          initialClk <- c.downField("initialClk").as[Option[String]]
          mc <- c.downField("mc").as[Option[Set[MarketChange]]]
          conflateMs <- c.downField("conflateMs").as[Option[Long]]
          segmentType <- c.downField("segmentType").as[Option[SegmentType]]
          status <- c.downField("status").as[Option[Int]]
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
        )
    },
    new Encoder[MarketChangeMessage] {
      override def apply(a: MarketChangeMessage): Json =
        Json.obj(
          ("op", Json.fromString("mcm")),
          ("id", a.id.asJson),
          ("ct", a.ct.asJson),
          ("clk", a.clk.asJson),
          ("heartbeatMs", a.heartbeatMs.asJson),
          ("pt", a.pt.asJson),
          ("initialClk", a.initialClk.asJson),
          ("mc", a.mc.asJson),
          ("conflateMs", a.conflateMs.asJson),
          ("segmentType", a.segmentType.asJson),
          ("status", a.status.asJson)
        )
    }
  )

  implicit val incomingMessageCodec: Codec[IncomingBetfairSocketMessage] = Codec.from(
    new Decoder[IncomingBetfairSocketMessage] {
      override def apply(c: HCursor): Result[IncomingBetfairSocketMessage] =
        c.downField("op").as[Op] match {
          case Right(op)   => decodeOp(op, c)
          case Left(error) => throw new IllegalStateException(s"failed to decode incoming message - because [$error]")
        }

      def decodeOp(op: Op, c: HCursor): Result[IncomingBetfairSocketMessage] =
        op match {
          case Op.Connection => c.value.as[Connection]
          case Op.mcm        => c.value.as[MarketChangeMessage]
          case Op.Status     => decodeStatus(c)
          case _             => throw new IllegalStateException(s"expected an incoming op but received '$op'")
        }

      def decodeStatus(c: HCursor): Result[IncomingBetfairSocketMessage] = {
        val statusCode = c.downField("statusCode").as[StatusCode] match {
          case Right(status) => status
          case Left(error) =>
            throw new IllegalStateException(
              s"failed to decode status code - expected 'op' to be either 'SUCCESS' or 'FAILURE' but [$error]"
            )
        }
        statusCode match {
          case StatusCode.Success =>
            log.info("Decoding success")
            c.downField("connectionClosed").as[ConnectionClosed].map(Success(_))
          case StatusCode.Failure =>
            log.info("Decoding Failure")
            val res = for {
              errorCode <- c.downField("errorCode").as[ErrorCode]
              errorMessage <- c.downField("errorMessage").as[ErrorMessage]
              connectionClosed <- c.downField("connectionClosed").as[ConnectionClosed]
              connectionId <- c.downField("connectionId").as[ConnectionId]
            } yield Failure(errorCode, errorMessage, connectionClosed, connectionId)
            log.info(s"failure => $res")
            res
        }
      }
    },
    new Encoder[IncomingBetfairSocketMessage] {
      override def apply(a: IncomingBetfairSocketMessage): Json =
        a match {
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
    }
  )

  implicit class JsonKeysOps(self: Option[Iterable[String]]) {
    def hasKey(key: String): Boolean =
      self.flatMap(_.find(_ == key)) match {
        case Some(_) => true
        case None    => false
      }

    def hasKeys(keys: String*): Boolean =
      keys.forall(hasKey(_))
  }

  implicit val outgoingMessageCodec: Codec[OutgoingBetfairSocketMessage] = Codec.from(
    new Decoder[OutgoingBetfairSocketMessage] {
      override def apply(c: HCursor): Result[OutgoingBetfairSocketMessage] = {
        log.info(s"decoding OutgoingBetfairSocketMessage [${c.value}]")
        c.downField("op").as[Op] match {
          case Right(op) => decodeOp(op, c)
          case Left(error) =>
            log.warn(s"failed to decode outgoing message op - because [$error]")
            throw new IllegalStateException(s"failed to decode outgoing message op - because [$error]")
        }
      }

      def decodeOp(op: Op, c: HCursor): Result[OutgoingBetfairSocketMessage] = {
        log.info(s"op is $op")
        op match {
          case Op.Authentication     => c.as[Authentication]
          case Op.MarketSubscription => c.as[MarketSubscription]
          case _                     => throw new IllegalStateException(s"expected an outgoing op but received '$op'")
        }
      }
    },
    new Encoder[OutgoingBetfairSocketMessage] {
      override def apply(a: OutgoingBetfairSocketMessage): Json =
        a match {
          case marketSubscription: MarketSubscription => marketSubscription.asJson
          case Heartbeat                              => Heartbeat.asJson
          case authentication: Authentication         => authentication.asJson
        }
    }
  )
}

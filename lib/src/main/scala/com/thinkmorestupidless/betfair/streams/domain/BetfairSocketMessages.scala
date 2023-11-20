package com.thinkmorestupidless.betfair.streams.domain

import enumeratum.{CirceEnum, Enum, EnumEntry}
import enumeratum.EnumEntry.{LowerCamelcase, Snakecase, UpperSnakecase}
import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, SessionToken}

sealed trait BetfairSocketMessage

sealed trait OutgoingBetfairSocketMessage extends BetfairSocketMessage
final case object Heartbeat extends OutgoingBetfairSocketMessage
abstract case class Authentication private (op: Op = Op.Authentication, appKey: ApplicationKey, session: SessionToken)
    extends OutgoingBetfairSocketMessage
object Authentication {
  def apply(appKey: ApplicationKey, session: SessionToken): Authentication =
    new Authentication(appKey = appKey, session = session) {}
}
abstract case class MarketSubscription(op: Op = Op.MarketSubscription, marketFilter: Option[MarketFilter])
    extends OutgoingBetfairSocketMessage
object MarketSubscription {
  val empty = MarketSubscription(maybeMarketFilter = None)

  def apply(marketFilter: MarketFilter): MarketSubscription =
    new MarketSubscription(marketFilter = Some(marketFilter)) {}

  def apply(maybeMarketFilter: Option[MarketFilter]): MarketSubscription =
    new MarketSubscription(marketFilter = maybeMarketFilter) {}
}

sealed trait IncomingBetfairSocketMessage extends BetfairSocketMessage
abstract case class Connection private (op: Op = Op.Connection, connectionId: ConnectionId)
    extends IncomingBetfairSocketMessage
object Connection {
  def apply(connectionId: ConnectionId): Connection =
    new Connection(connectionId = connectionId) {}
}
final case class Success(connectionClosed: ConnectionClosed) extends IncomingBetfairSocketMessage
final case class Failure(
    errorCode: ErrorCode,
    errorMessage: ErrorMessage,
    connectionClosed: ConnectionClosed,
    connectionId: ConnectionId
) extends IncomingBetfairSocketMessage
final case class MarketChangeMessage(
    op: Op = Op.mcm,
    id: Option[Int],
    ct: Option[ChangeType],
    clk: String,
    heartbeatMs: Option[Long],
    pt: Long,
    initialClk: Option[String],
    mc: Option[Set[MarketChange]],
    conflateMs: Option[Long],
    segmentType: Option[SegmentType],
    status: Option[Int]
) extends IncomingBetfairSocketMessage
object MarketChangeMessage {
  def apply(
      id: Option[Int],
      ct: Option[ChangeType],
      clk: String,
      heartbeatMs: Option[Long],
      pt: Long,
      initialClk: Option[String],
      mc: Option[Set[MarketChange]],
      conflateMs: Option[Long],
      segmentType: Option[SegmentType],
      status: Option[Int]
  ): MarketChangeMessage =
    new MarketChangeMessage(Op.mcm, id, ct, clk, heartbeatMs, pt, initialClk, mc, conflateMs, segmentType, status)
}

final case class CannotParseJson(cause: Throwable) extends IncomingBetfairSocketMessage
case object SocketReady extends IncomingBetfairSocketMessage
case object SocketFailed extends IncomingBetfairSocketMessage

final case class ErrorMessage(value: String)
final case class ConnectionClosed(value: Boolean)
final case class ConnectionId(value: String)

sealed trait IncomingOp
sealed trait OutgoingOp

sealed trait Op extends EnumEntry with LowerCamelcase
object Op extends Enum[Op] with CirceEnum[Op] {
  val values = findValues

  case object Connection extends Op
  case object Authentication extends Op
  case object Status extends Op
  case object MarketSubscription extends Op
  case object mcm extends Op
}

sealed trait StatusCode extends EnumEntry with UpperSnakecase
object StatusCode extends Enum[StatusCode] with CirceEnum[StatusCode] {
  val values = findValues

  case object Failure extends StatusCode
  case object Success extends StatusCode
}

sealed trait ErrorCode extends EnumEntry with UpperSnakecase
object ErrorCode extends Enum[ErrorCode] with CirceEnum[ErrorCode] {
  val values = findValues

  case object Timeout extends ErrorCode
  case object NoAppKey extends ErrorCode
  case object InvalidAppKey extends ErrorCode
  case object InvalidInput extends ErrorCode
  case object SubscriptionLimitExceeded extends ErrorCode
}

sealed trait ChangeType extends EnumEntry with UpperSnakecase
object ChangeType extends Enum[ChangeType] with CirceEnum[ChangeType] {
  val values = findValues

  case object SubImage extends ChangeType
  case object ResubDelta extends ChangeType
  case object Heartbeat extends ChangeType
}

sealed trait SegmentType extends EnumEntry with UpperSnakecase
object SegmentType extends Enum[SegmentType] with CirceEnum[SegmentType] {
  val values = findValues

  case object SegStart extends SegmentType
  case object Seg extends SegmentType
  case object SegEnd extends SegmentType
}

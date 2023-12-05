package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.streams.domain.{IncomingBetfairSocketMessage, OutgoingBetfairSocketMessage}
import com.thinkmorestupidless.betfair.streams.impl.JsonCodecs._
import io.circe.parser._
import io.circe.syntax._
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.{BidiFlow, Flow}
import org.apache.pekko.util.ByteString
import org.slf4j.LoggerFactory

object BetfairCodeBidiFlow {

  type BetfairCodecBidiFlow =
    BidiFlow[OutgoingBetfairSocketMessage, ByteString, ByteString, IncomingBetfairSocketMessage, NotUsed]

  private val log = LoggerFactory.getLogger(getClass)

  private val outgoing =
    Flow[OutgoingBetfairSocketMessage].map(_.asJson.noSpaces).map(str => ByteString(s"$str\r\n"))

  private val incoming = Flow[ByteString]
    .map(_.utf8String)
    .map { str =>
      for {
        json <- parse(str)
        msg <- json.as[IncomingBetfairSocketMessage]
      } yield msg
    }
    .collect {
      case Right(message) => Some(message)
      case Left(error) =>
        log.error(s"Failed to process incoming message '$error'")
        None
    }
    .collect { case Some(message) =>
      message
    }

  def apply(): BetfairCodecBidiFlow =
    BidiFlow.fromFlows(outgoing, incoming)
}

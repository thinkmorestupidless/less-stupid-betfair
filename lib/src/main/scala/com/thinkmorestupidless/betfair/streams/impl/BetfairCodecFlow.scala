package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.streams.domain.{IncomingBetfairSocketMessage, OutgoingBetfairSocketMessage}
import com.thinkmorestupidless.betfair.streams.impl.JsonCodecs._
import io.circe.parser._
import io.circe.syntax._
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.{BidiFlow, Flow}
import org.apache.pekko.util.ByteString
import org.slf4j.LoggerFactory

object BetfairCodecFlow {

  type BetfairCodecFlow =
    BidiFlow[OutgoingBetfairSocketMessage, ByteString, ByteString, IncomingBetfairSocketMessage, NotUsed]

  private val log = LoggerFactory.getLogger(getClass)

  private val outgoing = Flow[OutgoingBetfairSocketMessage].map(msg => ByteString(s"${msg.asJson.noSpaces}\n"))

  private val incoming = Flow[ByteString]
    .map(byteString =>
      for {
        json <- parse(byteString.utf8String)
        msg <- json.as[IncomingBetfairSocketMessage]
      } yield msg
    )
    .collect {
      case Right(message) => Some(message)
      case Left(error) =>
        log.error(s"Failed to process incoming message '$error'")
        None
    }
    .collect { case Some(message) =>
      message
    }

  def apply(): BetfairCodecFlow =
    BidiFlow.fromFlows(outgoing, incoming)
}

package com.thinkmorestupidless.betfair.streams.impl

import akka.NotUsed
import akka.stream.scaladsl.{BidiFlow, Flow}
import akka.util.ByteString
import com.thinkmorestupidless.betfair.streams.domain.{IncomingBetfairSocketMessage, OutgoingBetfairSocketMessage}
import io.circe.Error
import io.circe.parser._
import io.circe.syntax._
import com.thinkmorestupidless.betfair.streams.impl.JsonCodecs._
import org.slf4j.LoggerFactory

object BetfairCodecFlow {

  type BetfairCodecFlow =
    BidiFlow[OutgoingBetfairSocketMessage, ByteString, ByteString, IncomingBetfairSocketMessage, NotUsed]

  private val log = LoggerFactory.getLogger(getClass)

  private val toByteString: OutgoingBetfairSocketMessage => ByteString =
    msg => ByteString(s"${msg.asJson.noSpaces}\n")

  private val fromByteString: ByteString => Either[Error, IncomingBetfairSocketMessage] =
    byteString => {
      val x = for {
        json <- parse(byteString.utf8String)
        msg <- json.as[IncomingBetfairSocketMessage]
      } yield msg
      x
    }

  def apply(): BetfairCodecFlow = {
    val outgoing = Flow[OutgoingBetfairSocketMessage].map(toByteString)
    val incoming = Flow[ByteString].map(fromByteString).collect {
      case Right(message) => Some(message)
      case Left(error) =>
        log.error(s"Failed to process incoming message '$error'")
        None
    }

    BidiFlow.fromFlows(outgoing, incoming)
  }
}

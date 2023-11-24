package com.thinkmorestupidless.betfair.exchange.impl

import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.ListEventTypes
import com.thinkmorestupidless.betfair.exchange.impl.JsonCodecs._
import io.circe.syntax._
import org.apache.pekko.util.ByteString
import play.api.libs.ws.{BodyWritable, InMemoryBody}

object BodyWritables {

  implicit val writableOf_ListEventTypes: BodyWritable[ListEventTypes] =
    BodyWritable(o => InMemoryBody(ByteString(o.asJson.noSpaces)), "application/json")
}

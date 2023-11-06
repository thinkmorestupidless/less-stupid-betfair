package com.thinkmorestupidless.betfair.exchange.impl

import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.ListEventTypes
import org.apache.pekko.util.ByteString
import play.api.libs.ws.{BodyWritable, InMemoryBody}
import io.circe.syntax._
import com.thinkmorestupidless.betfair.exchange.impl.JsonCodecs._

object BodyWritables {

  implicit val writableOf_ListEventTypes: BodyWritable[ListEventTypes] =
    BodyWritable(o => InMemoryBody(ByteString(o.asJson.noSpaces)), "application/json")
}

package com.thinkmorestupidless.betfair.streams.impl.grpc

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.proto.streams.{BetfairStreamsService, MarketChange, SubscribeToMarketChangesRequest}
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source

final class GrpcStreamsServiceImpl(betfair: Betfair)
    extends BetfairStreamsService {
  override def subscribeToMarketChanges(in: SubscribeToMarketChangesRequest): Source[MarketChange, NotUsed] = ???
}

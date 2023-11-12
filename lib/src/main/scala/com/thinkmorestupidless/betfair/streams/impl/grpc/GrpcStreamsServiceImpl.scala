package com.thinkmorestupidless.betfair.streams.impl.grpc

import com.thinkmorestupidless.betfair.proto.streams.{
  BetfairStreamsService,
  MarketChange,
  SubscribeToMarketChangesRequest
}
import com.thinkmorestupidless.betfair.streams.domain.usecases.SubscribeToMarketChangesUseCase.SubscribeToMarketChangesUseCase
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source

final class GrpcStreamsServiceImpl(subscribeToMarketChanges: SubscribeToMarketChangesUseCase)
    extends BetfairStreamsService {
  override def subscribeToMarketChanges(in: SubscribeToMarketChangesRequest): Source[MarketChange, NotUsed] = ???
}

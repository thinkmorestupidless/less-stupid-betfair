package com.thinkmorestupidless.betfair.streams.impl.grpc

import com.thinkmorestupidless.betfair.Betfair
import com.thinkmorestupidless.betfair.proto.streams.{
  BetfairStreamsService,
  MarketChange,
  SubscribeToMarketChangesRequest
}
import com.thinkmorestupidless.betfair.streams.domain.{MarketChangeMessage, MarketFilter, MarketSubscription}
import com.thinkmorestupidless.betfair.streams.impl.grpc.Decoders._
import com.thinkmorestupidless.betfair.streams.impl.grpc.Encoders._
import com.thinkmorestupidless.grpc.Decoder._
import com.thinkmorestupidless.grpc.Encoder._
import com.thinkmorestupidless.utils.ValidationException
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.Source

final class GrpcStreamsServiceImpl(betfair: Betfair)(implicit system: ActorSystem[_]) extends BetfairStreamsService {

  override def subscribeToMarketChanges(in: SubscribeToMarketChangesRequest): Source[MarketChange, NotUsed] =
    in.decode.fold(
      errors => Source.failed(ValidationException.combineErrors(errors)),
      decoded => {
//        val (sink, source) = betfair.socketFlow.sinkAndSource()
        Source.single(decoded.marketFilter).map(MarketSubscription(_)).runWith(betfair.socketFlow.sink)
        betfair.socketFlow.source
          .collect { case MarketChangeMessage(_, _, _, _, _, _, _, Some(marketChanges), _, _, _) =>
            marketChanges
          }
          .flatMapConcat { elem =>
            Source(elem)
          }
          .map(x => x.encode)
      }
    )
}

object GrpcStreamsServiceImpl {
  final case class SubscribeToMarketChangesRequest(marketFilter: MarketFilter)
}

package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.streams.domain.{GlobalMarketFilterRepository, MarketFilter}
import com.thinkmorestupidless.betfair.streams.impl.MarketFilterUtils._

import scala.concurrent.{ExecutionContext, Future}

final class InMemoryMarketFilterRepository()(implicit ec: ExecutionContext) extends GlobalMarketFilterRepository {

  private var maybeFilter: Option[MarketFilter] = None

  override def upsertGlobalMarketFilter(marketFilter: MarketFilter): Future[Unit] =
    Future {
      maybeFilter = Some(maybeFilter.map(_.mergeWith(marketFilter)).getOrElse(marketFilter))
      ()
    }

  override def getCurrentGlobalFilter(): Future[MarketFilter] =
    Future.successful(maybeFilter.getOrElse(MarketFilter.empty))
}

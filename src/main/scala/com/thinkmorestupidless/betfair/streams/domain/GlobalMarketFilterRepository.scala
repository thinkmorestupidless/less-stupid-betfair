package com.thinkmorestupidless.betfair.streams.domain

import scala.concurrent.Future

trait GlobalMarketFilterRepository {

  def upsertGlobalMarketFilter(marketFilter: MarketFilter): Future[Unit]

  def getCurrentGlobalFilter(): Future[MarketFilter]
}

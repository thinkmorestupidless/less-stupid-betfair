package com.thinkmorestupidless.betfair.strategy.domain

import com.thinkmorestupidless.betfair.streams.domain.{MarketChange, MarketFilter}
import org.apache.pekko.Done

import scala.concurrent.{ExecutionContext, Future}

trait BettingStrategyOrchestrator {

  def onMarketChange(marketChange: MarketChange): Unit

  def registerBettingStrategy(bettingStrategy: BettingStrategy): Unit
}

trait BettingStrategy {

  val name: String

  val marketFilter: MarketFilter

  def logic(): BettingStrategyLogic
}

trait BettingStrategyLogic {

  def onMarketChange(marketChange: MarketChange)(implicit ec: ExecutionContext): Future[Done]
}

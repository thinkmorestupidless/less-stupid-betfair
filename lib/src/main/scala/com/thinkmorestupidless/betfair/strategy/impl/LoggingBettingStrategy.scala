package com.thinkmorestupidless.betfair.strategy.impl

import com.thinkmorestupidless.betfair.strategy.domain.{BettingStrategy, BettingStrategyLogic}
import com.thinkmorestupidless.betfair.streams.domain.{MarketChange, MarketFilter}
import org.apache.pekko.Done
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

object LoggingBettingStrategy {

  private val log = LoggerFactory.getLogger(getClass)

  def apply(filter: MarketFilter): BettingStrategy =
    new BettingStrategy {
      override val name: String = "logging-strategy"
      override val marketFilter: MarketFilter = filter

      override def logic(): BettingStrategyLogic =
        new BettingStrategyLogic {
          override def onMarketChange(marketChange: MarketChange)(implicit ec: ExecutionContext): Future[Done] =
            Future {
              log.info(s"strategy triggered by '$marketChange'")
              Done
            }
        }
    }
}

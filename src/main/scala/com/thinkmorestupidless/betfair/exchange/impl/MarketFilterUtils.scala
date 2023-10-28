package com.thinkmorestupidless.betfair.exchange.impl

import com.thinkmorestupidless.betfair.exchange.domain.MarketFilter

object MarketFilterUtils {

  implicit class MarketFilterCompanionOps(self: MarketFilter.type) {
    def empty(): MarketFilter =
      MarketFilter()
  }
}

package com.thinkmorestupidless.betfair.exchange.usecases

import cats.data.EitherT
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.EventTypeResponse
import com.thinkmorestupidless.betfair.exchange.usecases.ListEventTypesUseCase.FailedToListEventTypes
import com.thinkmorestupidless.betfair.exchange.domain.MarketFilter
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

object ListAllEventTypesUseCase {

  private val log = LoggerFactory.getLogger(getClass)

  type ListAllEventTypesUseCase = () => EitherT[Future, FailedToListEventTypes, List[EventTypeResponse]]

  def apply(exchange: BetfairExchangeService)(implicit ec: ExecutionContext): ListAllEventTypesUseCase = {
    log.info("getting all event types")
    () => exchange.listEventTypes(MarketFilter.empty).leftMap(FailedToListEventTypes(_))
  }
}
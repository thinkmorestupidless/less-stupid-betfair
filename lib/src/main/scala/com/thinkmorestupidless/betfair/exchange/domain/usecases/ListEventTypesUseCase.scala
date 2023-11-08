package com.thinkmorestupidless.betfair.exchange.domain.usecases

import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.exchange.domain.{BetfairExchangeService, EventType, MarketFilter}

import scala.concurrent.{ExecutionContext, Future}

object ListEventTypesUseCase {

  case object FailedToListEventTypes

  type ListEventTypesUseCase = MarketFilter => Future[Either[FailedToListEventTypes.type, List[EventType]]]

  def apply(
      betfairExchangeService: BetfairExchangeService
  )(implicit session: BetfairSession, ec: ExecutionContext): ListEventTypesUseCase =
    marketFilter =>
      betfairExchangeService.listEventTypes(marketFilter).map(_.map(_.eventType)).map(Right(_)).recover {
        case e: Exception => Left(FailedToListEventTypes)
      }
}

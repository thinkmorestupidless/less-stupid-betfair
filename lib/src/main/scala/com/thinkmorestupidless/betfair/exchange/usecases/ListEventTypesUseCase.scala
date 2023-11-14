package com.thinkmorestupidless.betfair.exchange.usecases

import cats.data.EitherT
import cats.syntax.either._
import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.auth.impl.SessionTokenStore
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.{EventTypeResponse, ExchangeServiceError}
import com.thinkmorestupidless.betfair.exchange.domain.{BetfairExchangeService, MarketFilter}

import scala.concurrent.{ExecutionContext, Future}

object ListEventTypesUseCase {

  final case class FailedToListEventTypes(cause: ExchangeServiceError)

  type ListEventTypesUseCase = MarketFilter => EitherT[Future, FailedToListEventTypes, List[EventTypeResponse]]

  def apply(exchange: BetfairExchangeService)(implicit ec: ExecutionContext): ListEventTypesUseCase =
    marketFilter => exchange.listEventTypes(marketFilter).leftMap(FailedToListEventTypes(_))
}

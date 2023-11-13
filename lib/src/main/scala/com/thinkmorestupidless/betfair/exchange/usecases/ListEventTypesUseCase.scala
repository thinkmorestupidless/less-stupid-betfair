package com.thinkmorestupidless.betfair.exchange.usecases

import cats.data.EitherT
import cats.syntax.either._
import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.EventTypeResponse
import com.thinkmorestupidless.betfair.exchange.domain.{BetfairExchangeService, MarketFilter}

import scala.concurrent.{ExecutionContext, Future}

object ListEventTypesUseCase {

  final case class FailedToListEventTypes(cause: Throwable) extends RuntimeException(cause)

  type ListEventTypesUseCase = MarketFilter => EitherT[Future, FailedToListEventTypes, List[EventTypeResponse]]

  def apply(
      exchange: BetfairExchangeService
  )(implicit session: BetfairSession, ec: ExecutionContext): ListEventTypesUseCase =
    marketFilter =>
      EitherT(exchange.listEventTypes(marketFilter).map(_.asRight).recover { case cause: Throwable =>
        FailedToListEventTypes(cause).asLeft
      })
}

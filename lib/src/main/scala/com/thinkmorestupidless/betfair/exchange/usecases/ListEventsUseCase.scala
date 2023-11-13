package com.thinkmorestupidless.betfair.exchange.usecases

import cats.data.EitherT
import cats.syntax.either._
import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.EventResponse
import com.thinkmorestupidless.betfair.exchange.domain.{BetfairExchangeService, MarketFilter}

import scala.concurrent.{ExecutionContext, Future}

object ListEventsUseCase {

  final case class FailedToListEvents(cause: Throwable) extends RuntimeException(cause)

  type ListEventsUseCase = MarketFilter => EitherT[Future, FailedToListEvents, List[EventResponse]]

  def apply(
      exchange: BetfairExchangeService
  )(implicit session: BetfairSession, ec: ExecutionContext): ListEventsUseCase =
    marketFilter =>
      EitherT(exchange.listEvents(marketFilter).map(_.toList.asRight).recover { case cause: Throwable =>
        FailedToListEvents(cause).asLeft
      })
}

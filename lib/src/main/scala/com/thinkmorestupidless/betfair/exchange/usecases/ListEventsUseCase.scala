package com.thinkmorestupidless.betfair.exchange.usecases

import cats.data.{EitherT, NonEmptyList}
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.{EventResponse, ExchangeServiceError}
import com.thinkmorestupidless.betfair.exchange.domain.{BetfairExchangeService, MarketFilter}
import com.thinkmorestupidless.utils.ValidationException
import com.thinkmorestupidless.utils.ValidationException.combineErrors

import scala.concurrent.{ExecutionContext, Future}

object ListEventsUseCase {

  final case class FailedToListEvents(error: ExchangeServiceError) {
    def toValidationException(): ValidationException =
      combineErrors(NonEmptyList.of(ValidationException("Failed to list Events"), error.toValidationException()))
  }

  type ListEventsUseCase = MarketFilter => EitherT[Future, FailedToListEvents, List[EventResponse]]

  def apply(exchange: BetfairExchangeService)(implicit ec: ExecutionContext): ListEventsUseCase =
    marketFilter => exchange.listEvents(marketFilter).leftMap(FailedToListEvents(_))
}

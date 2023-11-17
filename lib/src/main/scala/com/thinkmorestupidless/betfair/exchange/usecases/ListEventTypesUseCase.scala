package com.thinkmorestupidless.betfair.exchange.usecases

import cats.data.{EitherT, NonEmptyList}
import com.thinkmorestupidless.betfair.exchange.domain.BetfairExchangeService.{EventTypeResponse, ExchangeServiceError}
import com.thinkmorestupidless.betfair.exchange.domain.{BetfairExchangeService, MarketFilter}
import com.thinkmorestupidless.utils.ValidationException
import com.thinkmorestupidless.utils.ValidationException.combineErrors

import scala.concurrent.{ExecutionContext, Future}

object ListEventTypesUseCase {

  type ListEventTypesUseCase = MarketFilter => EitherT[Future, FailedToListEventTypes, List[EventTypeResponse]]

  final case class FailedToListEventTypes(error: ExchangeServiceError) {
    def toValidationException(): ValidationException =
      combineErrors(NonEmptyList.of(ValidationException("Failed to list EventTypes"), error.toValidationException()))
  }

  def apply(exchange: BetfairExchangeService)(implicit ec: ExecutionContext): ListEventTypesUseCase =
    marketFilter => exchange.listEventTypes(marketFilter).leftMap(FailedToListEventTypes(_))
}

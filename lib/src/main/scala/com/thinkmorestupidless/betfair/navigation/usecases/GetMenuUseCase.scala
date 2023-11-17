package com.thinkmorestupidless.betfair.navigation.usecases

import cats.data.{EitherT, NonEmptyList}
import com.thinkmorestupidless.betfair.navigation.domain.BetfairNavigationService.NavigationServiceError
import com.thinkmorestupidless.betfair.navigation.domain.{BetfairNavigationService, GetMenuRequest, Menu}
import com.thinkmorestupidless.utils.ValidationException
import com.thinkmorestupidless.utils.ValidationException.combineErrors

import scala.concurrent.{ExecutionContext, Future}

object GetMenuUseCase {

  type GetMenuUseCase = GetMenuRequest => EitherT[Future, FailedToGetMenu, Menu]

  final case class FailedToGetMenu(error: NavigationServiceError) {
    def toValidationException(): ValidationException =
      combineErrors(NonEmptyList.of(ValidationException("Failed to get Menu"), error.toValidationException()))
  }

  def apply(betfairNavigationService: BetfairNavigationService)(implicit ec: ExecutionContext): GetMenuUseCase =
    _ => betfairNavigationService.menu().leftMap(FailedToGetMenu(_))
}

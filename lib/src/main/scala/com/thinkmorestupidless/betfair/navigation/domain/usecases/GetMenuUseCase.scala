package com.thinkmorestupidless.betfair.navigation.domain.usecases

import cats.data.EitherT
import com.thinkmorestupidless.betfair.navigation.domain.{BetfairNavigationService, GetMenuRequest, Menu}
import com.thinkmorestupidless.utils.ValidationException

import scala.concurrent.{ExecutionContext, Future}

object GetMenuUseCase {

  type GetMenuUseCase = GetMenuRequest => EitherT[Future, ValidationException, Menu]

  def apply(
      betfairNavigationService: BetfairNavigationService
  )(implicit ec: ExecutionContext): GetMenuUseCase =
    _ => betfairNavigationService.menu().leftMap(_.toValidationException())
}

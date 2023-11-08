package com.thinkmorestupidless.betfair.navigation.domain.usecases

import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.navigation.domain.{BetfairNavigationService, GetMenuRequest, Menu}
import com.thinkmorestupidless.utils.ValidationException

import scala.concurrent.{ExecutionContext, Future}

object GetMenuUseCase {

  type GetMenuUseCase = GetMenuRequest => Future[Either[ValidationException, Menu]]

  def apply(
      betfairNavigationService: BetfairNavigationService
  )(implicit session: BetfairSession, ec: ExecutionContext): GetMenuUseCase =
    _ =>
      betfairNavigationService.menu().map(Right(_)).recover { case e: Exception =>
        Left(ValidationException("Failed to get Betfair Menu", Some(e)))
      }
}

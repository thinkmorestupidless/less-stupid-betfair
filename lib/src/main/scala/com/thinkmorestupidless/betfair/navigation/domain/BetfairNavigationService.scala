package com.thinkmorestupidless.betfair.navigation.domain

import cats.data.EitherT
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.AuthenticationError
import com.thinkmorestupidless.betfair.navigation.domain.BetfairNavigationService.NavigationServiceError
import com.thinkmorestupidless.utils.ValidationException

import scala.concurrent.Future

trait BetfairNavigationService {

  def menu(): EitherT[Future, NavigationServiceError, Menu]
}

object BetfairNavigationService {

  sealed trait NavigationServiceError {
    def toValidationException(): ValidationException =
      NavigationServiceError.toValidationException(this)
  }
  object NavigationServiceError {
    def toValidationException(error: NavigationServiceError): ValidationException =
      error match {
        case FailedAuthentication(authenticationError) => authenticationError.toValidationException()
        case UnexpectedParsingError(cause) =>
          ValidationException("unexpected error parsing navigation response", Some(cause))
        case UnexpectedNavigationError(cause) =>
          ValidationException("unexpected error from navigation API call", Some(cause))
      }
  }
  final case class FailedAuthentication(error: AuthenticationError) extends NavigationServiceError
  final case class UnexpectedParsingError(cause: Throwable) extends NavigationServiceError
  final case class UnexpectedNavigationError(cause: Throwable) extends NavigationServiceError
}

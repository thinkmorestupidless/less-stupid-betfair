package com.thinkmorestupidless.betfair.vendor.usecases

import cats.data.EitherT

import scala.concurrent.Future

object ExchangeCodeForToken {

  type ExchangeCodeForToken = () => EitherT[Future, _, _]
}

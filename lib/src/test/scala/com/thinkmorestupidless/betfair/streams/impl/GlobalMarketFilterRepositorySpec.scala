package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.utils.FutureSupport
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class GlobalMarketFilterRepositorySpec
    extends AnyFunSpecLike
    with Matchers
    with FutureSupport
    with GlobalMarketFilterRepositoryBehaviour {

  describe("InMemoryMarketFilterRepository") {
    val repositoryFactory = () => new InMemoryMarketFilterRepository()(ExecutionContext.global)
    (it should behave).like(globalMarketFilterRepositoryBehaviour(repositoryFactory))
  }
}

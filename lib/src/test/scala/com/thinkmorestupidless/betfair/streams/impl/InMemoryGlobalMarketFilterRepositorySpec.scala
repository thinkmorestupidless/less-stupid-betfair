package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.utils.FutureSupport
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class InMemoryGlobalMarketFilterRepositorySpec
    extends AnyFunSpecLike
    with Matchers
    with FutureSupport
    with GlobalMarketFilterRepositoryBehaviour {

  describe("InMemoryMarketFilterRepository") {
    val repositoryFactory = () => InMemoryMarketFilterRepository()(ExecutionContext.global)
    (it should behave).like(globalMarketFilterRepositoryBehaviour(repositoryFactory))
  }
}

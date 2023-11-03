package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.streams.domain.{GlobalMarketFilterRepository, MarketFilter, MarketId}
import com.thinkmorestupidless.utils.FutureSupport
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

trait GlobalMarketFilterRepositoryBehaviour {
  this: AnyFunSpecLike with Matchers with FutureSupport =>

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))

  def globalMarketFilterRepositoryBehaviour(repositoryFactory: () => GlobalMarketFilterRepository): Unit = {
    describe("getCurrentGlobalFilter") {
      it("should return an empty market filter if none already set") {
        val repository = repositoryFactory()
        val returnedMarketFilter = await(repository.getCurrentGlobalFilter())

        returnedMarketFilter shouldBe MarketFilter.empty
      }
    }

    describe("upsertGlobalMarketFilter") {
      it("should return the passed market filter on first upsert") {
        val repository = repositoryFactory()
        val expectedFilter = MarketFilter(List(MarketId("foo"), MarketId("bar")))

        await(repository.upsertGlobalMarketFilter(expectedFilter))

        val returnedFilter = await(repository.getCurrentGlobalFilter())

        returnedFilter shouldBe expectedFilter
      }

      it(
        "should return the result of merging existing market filter with passed market filter on subsequent upsert(s)"
      ) {
        val repository = repositoryFactory()
        val firstFilter = MarketFilter(List(MarketId("alice"), MarketId("bob")))
        val secondFilter = MarketFilter(List(MarketId("charlie")))
        val expectedFilter = MarketFilter(List(MarketId("alice"), MarketId("bob"), MarketId("charlie")))

        await(repository.upsertGlobalMarketFilter(firstFilter))

        await(repository.getCurrentGlobalFilter()) shouldBe firstFilter

        await(repository.upsertGlobalMarketFilter(secondFilter))

        val returnedFilter = await(repository.getCurrentGlobalFilter())

        returnedFilter shouldBe expectedFilter
      }
    }
  }
}

package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.streams.impl.less.stupid.betting.betfair.socket.impl.SlickMarketFilterRepository
import com.thinkmorestupidless.utils.{FutureSupport, IntegrationSpec, TruncatedTables}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.matchers.should.Matchers

final class SlickMarketFilterRepositorySpec
    extends AnyFunSpecLike
    with Matchers
    with FutureSupport
    with GlobalMarketFilterRepositoryBehaviour
    with IntegrationSpec
    with BeforeAndAfterEach
    with TruncatedTables {

  describe("SlickMarketFilterRepository") {
    val repositoryFactory = () => SlickMarketFilterRepository()
    (it should behave).like(globalMarketFilterRepositoryBehaviour(repositoryFactory))
  }
}

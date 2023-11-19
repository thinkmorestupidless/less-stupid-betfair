package com.thinkmorestupidless.betfair.auth.impl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.{FailedToParseLoginResponseAsJson, LoginRejectedByBetfair, UnexpectedLoginError}
import com.thinkmorestupidless.betfair.auth.domain.{LoginStatus, SessionToken}
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.utils.{ConfigSupport, FutureSupport}
import io.circe.CursorOp.DownField
import io.circe.{DecodingFailure, ParsingFailure}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.matchers.should.Matchers

final class PlayWsBetfairAuthenticationServiceSpec
    extends TestKit(ActorSystem("AkkaHttpBetfairAuthenticationServiceSpec"))
    with AnyFunSpecLike
    with Matchers
    with BeforeAndAfterAll
    with FutureSupport {

  protected val httpServer: WireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  override def beforeAll(): Unit =
    httpServer.start()

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    httpServer.stop()
  }

  describe("login") {

    it("should return a LoginError if response is not valid JSON") {
      val betfairConfig = generateConfig()

      val responseBody = """{"loginStatus: }"""
      createStubMapping(betfairConfig, responseBody)

      val authenticator = PlayWsBetfairAuthenticationService(betfairConfig, NoOpSessionTokenTokenStore)
      val error = awaitLeft(authenticator.login())

      error shouldBe a[FailedToParseLoginResponseAsJson]
    }

    it("should return a LoginError if response.loginStatus is not a member of the LoginStatus Enum") {
      val betfairConfig = generateConfig()

      val responseBody = """{"loginStatus":"GIGANTIC_MAN_EATING_TIGER"}"""
      createStubMapping(betfairConfig, responseBody)

      val authenticator = PlayWsBetfairAuthenticationService(betfairConfig, NoOpSessionTokenTokenStore)
      val error = awaitLeft(authenticator.login())

      error shouldBe a[UnexpectedLoginError]
    }

    it("should return a LoginError if rejected by Betfair") {
      val betfairConfig = generateConfig()

      createStubMapping(betfairConfig, """{"loginStatus":"ACCOUNT_PENDING_PASSWORD_CHANGE"}""")

      val authenticator = PlayWsBetfairAuthenticationService(betfairConfig, NoOpSessionTokenTokenStore)
      val error = awaitLeft(authenticator.login())

      error shouldBe LoginRejectedByBetfair(LoginStatus.AccountPendingPasswordChange)
    }

    it("should return a session token when successful") {
      val betfairConfig = generateConfig()

      createStubMapping(betfairConfig, """{"loginStatus":"SUCCESS","sessionToken":"abcd1234"}""")

      val authenticator = PlayWsBetfairAuthenticationService(betfairConfig, NoOpSessionTokenTokenStore)
      val result = awaitRight(authenticator.login())

      result shouldBe SessionToken("abcd1234")
    }
  }

  private def generateConfig(): BetfairConfig =
    ConfigSupport.generateConfig(baseUri = httpServer.baseUrl())

  private def createStubMapping(config: BetfairConfig, responseBody: String): Unit = {
    val loginUrl = config.auth.uri.value
    httpServer.stubFor(
      post(urlEqualTo(loginUrl.substring(loginUrl.lastIndexOf("/"))))
        .withRequestBody(equalTo("username=bigjohn&password=changeme"))
        .withHeader("Content-Type", matching("application/x-www-form-urlencoded"))
        .willReturn(
          aResponse().withBody(responseBody).withHeader("Content-Type", "text/plain; charset=ISO-8859-1")
        )
    )
  }
}

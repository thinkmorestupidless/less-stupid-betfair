package com.thinkmorestupidless.betfair.auth.impl

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LogCapturing}
import akka.actor.typed.scaladsl.adapter._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService.{
  FailedToParseLoginResponseAsJson,
  LoginRejectedByBetfair,
  UnexpectedLoginError
}
import com.thinkmorestupidless.betfair.auth.domain.{LoginStatus, SessionToken}
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import com.thinkmorestupidless.utils.{ConfigSupport, FutureSupport}
import io.circe.CursorOp.DownField
import io.circe.DecodingFailure
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.matchers.should.Matchers

final class AkkaHttpBetfairAuthenticationServiceSpec
    extends AnyFunSpecLike
    with Matchers
    with BeforeAndAfterAll
    with FutureSupport
    with LogCapturing {

  protected val httpServer: WireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  private val testkit = ActorTestKit()

  protected implicit val system = testkit.system.toClassic

  override def beforeAll(): Unit =
    httpServer.start()

  override def afterAll(): Unit = {
    testkit.shutdownTestKit()
    httpServer.stop()
  }

  describe("login") {

    it("should return a LoginError if response is not valid JSON") {
      val betfairConfig = generateConfig()

      val responseBody = """{"loginStatus: }"""
      createStubMapping(betfairConfig, responseBody)

      val authenticator = new AkkaHttpBetfairAuthenticationService(betfairConfig)
      val error = awaitLeft(authenticator.login())

      error shouldBe FailedToParseLoginResponseAsJson(responseBody, "exhausted input")
    }

    it("should return a LoginError if response.loginStatus is not a member of the LoginStatus Enum") {
      val betfairConfig = generateConfig()

      val responseBody = """{"loginStatus":"GIGANTIC_MAN_EATING_TIGER"}"""
      createStubMapping(betfairConfig, responseBody)

      val authenticator = new AkkaHttpBetfairAuthenticationService(betfairConfig)
      val error = awaitLeft(authenticator.login())

      val failure = DecodingFailure(
        s"'GIGANTIC_MAN_EATING_TIGER' is not a member of enum $LoginStatus",
        List(DownField("loginStatus"))
      )
      val expectedErrorMessage = s"failed to decode login status [$failure]"

      error shouldBe UnexpectedLoginError(expectedErrorMessage)
    }

    it("should return a LoginError if rejected by Betfair") {
      val betfairConfig = generateConfig()

      createStubMapping(betfairConfig, """{"loginStatus":"ACCOUNT_PENDING_PASSWORD_CHANGE"}""")

      val authenticator = new AkkaHttpBetfairAuthenticationService(betfairConfig)
      val error = awaitLeft(authenticator.login())

      error shouldBe LoginRejectedByBetfair(LoginStatus.AccountPendingPasswordChange)
    }

    it("should return a session token when successful") {
      val betfairConfig = generateConfig()

      createStubMapping(betfairConfig, """{"loginStatus":"SUCCESS","sessionToken":"abcd1234"}""")

      val authenticator = new AkkaHttpBetfairAuthenticationService(betfairConfig)
      val result = awaitRight(authenticator.login())

      result shouldBe SessionToken("abcd1234")
    }
  }

  private def generateConfig(): BetfairConfig =
    ConfigSupport.generateConfig(baseUri = httpServer.baseUrl())

  private def createStubMapping(config: BetfairConfig, responseBody: String): Unit = {
    val loginUrl = config.login.uri.value
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

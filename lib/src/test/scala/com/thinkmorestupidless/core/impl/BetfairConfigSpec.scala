package com.thinkmorestupidless.core.impl

import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, BetfairCredentials, Password, Username}
import com.thinkmorestupidless.betfair.core.impl._
import com.thinkmorestupidless.core.impl.BetfairConfigSpec._
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig._

final class BetfairConfigSpec extends AnyWordSpecLike with Matchers {

  "apply" should {
    "load betfair config" in {

      val productionConfig = ConfigFactory
        .load()
        .withValue("betfair.login.cert.file", ConfigValueFactory.fromAnyRef("betfair-cert-file"))
        .withValue("betfair.login.cert.password", ConfigValueFactory.fromAnyRef("betfair-cert-password"))
        .withValue("betfair.login.credentials.username", ConfigValueFactory.fromAnyRef("betfair-username"))
        .withValue("betfair.login.credentials.password", ConfigValueFactory.fromAnyRef("betfair-password"))
        .withValue(
          "betfair.login.credentials.application-key",
          ConfigValueFactory.fromAnyRef("betfair-application-key")
        )

      val config = ConfigSource.fromConfig(productionConfig).at(namespace = "betfair").load[BetfairConfig]

      config shouldBe Right(expectedBetfairConfig)
    }
  }
}

object BetfairConfigSpec {

  val expectedBetfairConfig = BetfairConfig(
    HeaderKeys(ApplicationKeyHeaderKey("X-Application"), SessionTokenHeaderKey("X-Authentication")),
    LoginConfig(
      Cert(CertFile("betfair-cert-file"), CertPassword("betfair-cert-password")),
      BetfairCredentials(
        Username("betfair-username"),
        Password("betfair-password"),
        ApplicationKey("betfair-application-key")
      ),
      LoginUri("https://identitysso-cert.betfair.com/api/certlogin")
    ),
    ExchangeConfig(
      List(
        RawHeader("Accept", "application/json"),
        RawHeader("Accept-Charset", "UTF-8"),
        RawHeader("Accept-Encoding", "gzip, deflate")
      ),
      SocketConfig(SocketUri("stream-api-integration.betfair.com"), SocketPort(443)),
      ExchangeUris(
        CancelOrdersUri("https://api.betfair.com/exchange/betting/rest/v1.0/cancelOrders/"),
        ListClearedOrdersUri("https://api.betfair.com/exchange/betting/rest/v1.0/listClearedOrders/"),
        ListCompetitionsUri("https://api.betfair.com/exchange/betting/rest/v1.0/listCompetitions/"),
        ListCountriesUri("https://api.betfair.com/exchange/betting/rest/v1.0/listCountries/"),
        ListCurrentOrdersUri("https://api.betfair.com/exchange/betting/rest/v1.0/listCurrentOrders/"),
        ListEventTypesUri("https://api.betfair.com/exchange/betting/rest/v1.0/listEventTypes/"),
        ListEventsUri("https://api.betfair.com/exchange/betting/rest/v1.0/listEvents/"),
        ListMarketCatalogueUri("https://api.betfair.com/exchange/betting/rest/v1.0/listMarketCatalogue/"),
        ListMarketBookUri("https://api.betfair.com/exchange/betting/rest/v1.0/listMarketBook/"),
        PlaceOrdersUri("https://api.betfair.com/exchange/betting/rest/v1.0/placeOrders/")
      )
    ),
    Navigation(MenuUri("https://api.betfair.com/exchange/betting/rest/v1.0/en/navigation/menu.json"))
  )
}

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

      val config = ConfigSource.fromConfig(ConfigFactory.load()).at(namespace = "betfair").load[BetfairConfig]

      config shouldBe Right(expectedBetfairConfig)
    }
  }
}

object BetfairConfigSpec {

  val expectedBetfairConfig = BetfairConfig(
    HeaderKeys(ApplicationKeyHeaderKey("X-Application"), SessionTokenHeaderKey("X-Authentication")),
    AuthConfig(
      Cert(CertFile("[BETFAIR_CERT_FILE MISSING]"), CertPassword("[BETFAIR_CERT_PASSWORD MISSING]")),
      BetfairCredentials(
        Username("[USERNAME MISSING]"),
        Password("[PASSWORD MISSING]"),
        ApplicationKey("[APPLICATION_KEY MISSING]")
      ),
      LoginUri("https://identitysso-cert.betfair.com/api/certlogin"),
      SessionStoreConfig(SessionStoreProviderType.None, FileProviderConfig(FileProviderFilePath(".")))
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
      ),
      ExchangeLogging(LogExchangeRequests(true), LogExchangeResponses(true))
    ),
    NavigationConfig(MenuUri("https://api.betfair.com/exchange/betting/rest/v1/en/navigation/menu.json"))
  )
}

package com.thinkmorestupidless.utils

import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, BetfairCredentials, Password, Username}
import com.thinkmorestupidless.betfair.core.impl._

object ConfigSupport {

  def generateConfig(baseUri: String) = BetfairConfig(
    HeaderKeys(ApplicationKeyHeaderKey("X-Application"), SessionTokenHeaderKey("X-Authentication")),
    AuthConfig(
      Cert(
        CertFile("certfile.pkcs12"),
        CertPassword("top-secret")
      ),
      BetfairCredentials(
        Username("bigjohn"),
        Password("changeme"),
        ApplicationKey("foobar")
      ),
      LoginUri(s"$baseUri/login"),
      SessionStoreConfig(SessionStoreProviderType.None, FileProviderConfig(FileProviderFilePath(".")))
    ),
    ExchangeConfig(
      List(
        RawHeader("Content-Type", "application/json"),
        RawHeader("Accept", "application/json"),
        RawHeader("Accept-Charset", "UTF-8"),
        RawHeader("Accept-Encoding", "gzip, deflate")
      ),
      SocketConfig(SocketUri("stream-api.betfair.com"), SocketPort(443)),
      ExchangeUris(
        CancelOrdersUri(s"$baseUri/cancelOrders/"),
        ListClearedOrdersUri(s"$baseUri/listClearedOrders/"),
        ListCompetitionsUri(s"$baseUri/listCompetitions/"),
        ListCountriesUri(s"$baseUri/listCountries"),
        ListCurrentOrdersUri(s"$baseUri/listCurrentOrders/"),
        ListEventTypesUri(s"$baseUri/listEventTypes"),
        ListEventsUri(s"$baseUri/listEvents"),
        ListMarketCatalogueUri(s"$baseUri/listMarketCatalogue"),
        ListMarketBookUri(s"$baseUri/listMarketBook"),
        PlaceOrdersUri(s"$baseUri/placeOrders")
      ),
      ExchangeLogging(LogExchangeRequests(true), LogExchangeResponses(true))
    ),
    NavigationConfig(MenuUri(s"$baseUri/navigation/menu.json"))
  )
}

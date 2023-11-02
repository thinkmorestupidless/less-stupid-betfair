package com.thinkmorestupidless.betfair.core.impl

import akka.http.scaladsl.model.headers.RawHeader
import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, BetfairCredentials, Password, Username}
import com.typesafe.config.Config
import pureconfig.generic.auto._
import pureconfig.{ConfigReader, ConfigSource}

final case class BetfairConfig(headerKeys: HeaderKeys, login: LoginConfig, exchange: ExchangeConfig)

final case class LoginConfig(cert: Cert, credentials: BetfairCredentials, uri: LoginUri)
final case class LoginUri(value: String)
final case class Cert(file: CertFile, password: CertPassword)
final case class CertFile(value: String)
final case class CertPassword(value: String)

final case class ExchangeConfig(requiredHeaders: List[RawHeader], socket: SocketConfig, uris: ExchangeUris)
final case class HeaderKeys(applicationKey: ApplicationKeyHeaderKey, sessionToken: SessionTokenHeaderKey)
final case class ApplicationKeyHeaderKey(value: String)
final case class SessionTokenHeaderKey(value: String)
final case class ExchangeUris(
    cancelOrders: CancelOrdersUri,
    listClearedOrders: ListClearedOrdersUri,
    listCompetitions: ListCompetitionsUri,
    listCountries: ListCountriesUri,
    listCurrentOrders: ListCurrentOrdersUri,
    listEventTypes: ListEventTypesUri,
    listEvents: ListEventsUri,
    listMarketCatalogue: ListMarketCatalogueUri,
    listMarketBook: ListMarketBookUri,
    placeOrders: PlaceOrdersUri
)
final case class AuthenticationUri(value: String)
final case class CancelOrdersUri(value: String)
final case class ListClearedOrdersUri(value: String)
final case class ListCompetitionsUri(value: String)
final case class ListCountriesUri(value: String)
final case class ListCurrentOrdersUri(value: String)
final case class ListEventTypesUri(value: String)
final case class ListEventsUri(value: String)
final case class ListMarketCatalogueUri(value: String)
final case class ListMarketBookUri(value: String)
final case class PlaceOrdersUri(value: String)
final case class SocketConfig(uri: SocketUri, port: SocketPort)
final case class SocketUri(value: String)
final case class SocketPort(value: Int)

object BetfairConfig {

  implicit val applicationKeyHeaderKeyReader = ConfigReader[String].map(ApplicationKeyHeaderKey(_))
  implicit val sessionTokenHeaderKeyReader = ConfigReader[String].map(SessionTokenHeaderKey(_))
  implicit val usernameReader = ConfigReader[String].map(Username(_))
  implicit val passwordReader = ConfigReader[String].map(Password(_))
  implicit val applicationKeyReader = ConfigReader[String].map(ApplicationKey(_))
  implicit val loginUriReader = ConfigReader[String].map(LoginUri(_))
  implicit val certFileReader = ConfigReader[String].map(CertFile(_))
  implicit val certPasswordReader = ConfigReader[String].map(CertPassword(_))
  implicit val socketUriReader = ConfigReader[String].map(SocketUri(_))
  implicit val socketPortReader = ConfigReader[Int].map(SocketPort(_))
  implicit val cancelOrdersReader = ConfigReader[String].map(CancelOrdersUri(_))
  implicit val listClearedOrdersReader = ConfigReader[String].map(ListClearedOrdersUri(_))
  implicit val listCompetitionsReader = ConfigReader[String].map(ListCompetitionsUri(_))
  implicit val listCountriesReader = ConfigReader[String].map(ListCountriesUri(_))
  implicit val listCurrentOrdersReader = ConfigReader[String].map(ListCurrentOrdersUri(_))
  implicit val listEventTypesReader = ConfigReader[String].map(ListEventTypesUri(_))
  implicit val listEventsReader = ConfigReader[String].map(ListEventsUri(_))
  implicit val listMarketCatalogueReader = ConfigReader[String].map(ListMarketCatalogueUri(_))
  implicit val listMarketBookReader = ConfigReader[String].map(ListMarketBookUri(_))
  implicit val placeOrdersReader = ConfigReader[String].map(PlaceOrdersUri(_))

  implicit val rawHeaderReader = ConfigReader.fromCursor[RawHeader] { cur =>
    for {
      key <- cur.fluent.at("key").asString
      value <- cur.fluent.at("value").asString
    } yield RawHeader(key, value)
  }

  def load(): ConfigReader.Result[BetfairConfig] =
    ConfigSource.default.at(namespace = "betfair").load[BetfairConfig]

  def load(config: Config): ConfigReader.Result[BetfairConfig] =
    ConfigSource.fromConfig(config).load[BetfairConfig]
}

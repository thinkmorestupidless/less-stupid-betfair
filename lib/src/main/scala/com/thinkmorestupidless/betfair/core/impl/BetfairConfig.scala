package com.thinkmorestupidless.betfair.core.impl

import com.thinkmorestupidless.betfair.auth.domain.{ApplicationKey, BetfairCredentials, Password, Username}
import enumeratum.EnumEntry.Hyphencase
import enumeratum.{Enum, EnumEntry}
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import pureconfig.generic.auto._
import pureconfig.{ConfigReader, ConfigSource}

final case class BetfairConfig(
    headerKeys: HeaderKeys,
    auth: AuthConfig,
    exchange: ExchangeConfig,
    navigation: NavigationConfig
)

final case class AuthConfig(
    cert: Cert,
    credentials: BetfairCredentials,
    uri: LoginUri,
    sessionStore: SessionStoreConfig
)
final case class LoginUri(value: String)
final case class Cert(file: CertFile, password: CertPassword)
final case class CertFile(value: String)
final case class CertPassword(value: String)

sealed trait SessionStoreProviderType extends EnumEntry with Hyphencase
case object SessionStoreProviderType extends Enum[SessionStoreProviderType] {
  override def values: IndexedSeq[SessionStoreProviderType] = findValues
  case object InMem extends SessionStoreProviderType
  case object File extends SessionStoreProviderType
}

final case class FileProviderConfig(filePath: FileProviderFilePath)
final case class FileProviderFilePath(value: String)
final case class SessionStoreConfig(providerType: SessionStoreProviderType, fileProvider: FileProviderConfig)

final case class ExchangeConfig(
    requiredHeaders: List[RawHeader],
    socket: SocketConfig,
    uris: ExchangeUris,
    logging: ExchangeLogging
)
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
final case class ExchangeLogging(logRequests: LogExchangeRequests, logResponses: LogExchangeResponses)
final case class LogExchangeRequests(value: Boolean)
final case class LogExchangeResponses(value: Boolean)
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
final case class NavigationConfig(uri: MenuUri)
final case class MenuUri(value: String)

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
  implicit val menuUriReader = ConfigReader[String].map(MenuUri(_))
  implicit val fileProviderFilePathReader = ConfigReader[String].map(FileProviderFilePath(_))
  implicit val sessionStoreProviderTypeReader =
    ConfigReader[String].map(SessionStoreProviderType.withNameInsensitive(_))
  implicit val logExchangeRequestsReader = ConfigReader[Boolean].map(LogExchangeRequests(_))
  implicit val logExchangeResponsesReader = ConfigReader[Boolean].map(LogExchangeResponses(_))

  implicit val rawHeaderReader = ConfigReader.fromCursor[RawHeader] { cur =>
    for {
      key <- cur.fluent.at("key").asString
      value <- cur.fluent.at("value").asString
    } yield RawHeader(key, value)
  }

  def load(): ConfigReader.Result[BetfairConfig] =
    ConfigSource.default.at(namespace = "betfair").load[BetfairConfig]
}

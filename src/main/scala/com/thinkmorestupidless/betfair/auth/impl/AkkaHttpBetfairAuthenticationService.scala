package com.thinkmorestupidless.betfair.auth.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{FormData, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import cats.data.EitherT
import cats.syntax.either._
import io.circe.Json
import io.circe.parser._
import com.thinkmorestupidless.betfair.auth.domain.BetfairAuthenticationService._
import com.thinkmorestupidless.betfair.auth.domain._
import com.thinkmorestupidless.betfair.auth.impl.JsonCodecs._
import com.thinkmorestupidless.betfair.core.impl.BetfairConfig
import org.slf4j.LoggerFactory

import scala.concurrent.Future

final class AkkaHttpBetfairAuthenticationService(config: BetfairConfig)(implicit system: ActorSystem)
    extends BetfairAuthenticationService {

  private implicit val ec = system.dispatcher

  private val log = LoggerFactory.getLogger(getClass)

  def login(): EitherT[Future, LoginError, SessionToken] = {
    val body =
      Map("username" -> config.login.credentials.username.value, "password" -> config.login.credentials.password.value)
    val entity = FormData(body).toEntity
    val headers = List(RawHeader(config.headerKeys.applicationKey.value, config.login.credentials.applicationKey.value))

    val request = Post(uri = config.login.uri.value, entity = entity).withHeaders(headers)

    for {
      response <- EitherT(Http().singleRequest(request).map(_.asRight))
      _ = log.info(s"response: $response")
      result <- handleLoginResponse(response)
    } yield result
  }

  private def handleLoginResponse(response: HttpResponse): EitherT[Future, LoginError, SessionToken] = {
    log.info(s"handling response: $response")
    EitherT {
      Unmarshal(response.entity).to[String].map(parseResponseString).recover { case e: Throwable =>
        Left(UnexpectedLoginError(e.getMessage()))
      }
    }
  }

  private def parseResponseString(bodyAsString: String): Either[LoginError, SessionToken] = {
    log.info(s"login response: $bodyAsString")
    parse(bodyAsString) match {
      case Right(json) => decodeJsonResponse(json)
      case Left(error) => FailedToParseLoginResponseAsJson(bodyAsString, error.getMessage()).asLeft
    }
  }

  private def decodeJsonResponse(json: Json): Either[LoginError, SessionToken] =
    json.as[LoginResponse] match {
      case Right(loginResponse) => matchLoginResponse(loginResponse)
      case Left(error)          => FailedToDecodeLoginResponseJson(json, error).asLeft
    }

  private def matchLoginResponse(loginResponse: LoginResponse): Either[LoginError, SessionToken] =
    loginResponse match {
      case LoginSuccess(sessionToken) => sessionToken.asRight
      case LoginFailure(loginStatus)  => LoginRejectedByBetfair(loginStatus).asLeft
    }

//  private def initialiseSslContext(config: Config): Unit = {
//    val certFile = config.getString("betfair.login.cert.file")
//    val password = config.getString("betfair.login.cert.password").toCharArray
//
//    val keyStore: KeyStore = KeyStore.getInstance("PKCS12")
//    keyStore.load(new FileInputStream(certFile), password)
//
//    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
//    keyManagerFactory.init(keyStore, password)
//
//    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
//    trustManagerFactory.init(keyStore)
//
//    val sslContext: SSLContext = SSLContext.getInstance("TLS")
//    sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom())
//
//    val https = ConnectionContext.httpsClient(sslContext)
//    Http().setDefaultClientHttpsContext(https)
//  }
}

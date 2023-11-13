package com.thinkmorestupidless.betfair.vendor.impl

import com.thinkmorestupidless.betfair.auth.domain.BetfairSession
import com.thinkmorestupidless.betfair.core.impl.{BetfairConfig, ClientId, ClientSecret}
import com.thinkmorestupidless.betfair.vendor.domain.BetfairVendorApi.TokenResponse
import com.thinkmorestupidless.betfair.vendor.domain.{BetfairVendorApi, Code, GrantType, RefreshToken}
import com.thinkmorestupidless.betfair.vendor.impl.AkkaHttpBetfairVendorApi.{RefreshTokenRequest, TokenExchangeRequest}
import com.thinkmorestupidless.betfair.vendor.impl.JsonCodecs._
import com.thinkmorestupidless.utils.CirceSupport
import io.circe.Decoder
import io.circe.parser.decode
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.client.RequestBuilding
import org.apache.pekko.http.scaladsl.coding.Coders
import org.apache.pekko.http.scaladsl.marshalling.ToEntityMarshaller
import org.apache.pekko.http.scaladsl.model.headers.{HttpEncodings, RawHeader}
import org.apache.pekko.http.scaladsl.model.{HttpHeader, HttpResponse}
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.{ExecutionContext, Future}

final class AkkaHttpBetfairVendorApi(config: BetfairConfig)(implicit system: ActorSystem)
    extends BetfairVendorApi
    with CirceSupport {

  private implicit val ec: ExecutionContext = system.dispatcher

  override def exchangeCodeForToken(code: Code)(implicit session: BetfairSession): Future[TokenResponse] =
    execute[TokenExchangeRequest, TokenResponse](
      TokenExchangeRequest(config.vendor.clientId, GrantType.AuthorizationCode, code, config.vendor.clientSecret),
      config.vendor.tokenUri.value
    )

  override def refreshToken(refreshToken: RefreshToken)(implicit session: BetfairSession): Future[TokenResponse] =
    execute[RefreshTokenRequest, TokenResponse](
      RefreshTokenRequest(config.vendor.clientId, GrantType.RefreshToken, refreshToken, config.vendor.clientSecret),
      config.vendor.tokenUri.value
    )

  private def execute[REQUEST, RESPONSE](content: REQUEST, uri: String)(implicit
      decoder: Decoder[RESPONSE],
      m: ToEntityMarshaller[REQUEST],
      session: BetfairSession
  ): Future[RESPONSE] = {
    val headers: Seq[HttpHeader] = config.exchange.requiredHeaders ++ List(
      RawHeader(config.headerKeys.applicationKey.value, session.applicationKey.value),
      RawHeader(config.headerKeys.sessionToken.value, session.sessionToken.value)
    )
    val request = RequestBuilding.Post(uri = uri, content = content).withHeaders(headers)
    for {
      response <- Http().singleRequest(request).map(decodeResponse)
      resultString <- Unmarshal(response.entity).to[String]
      _ = println(resultString)
      result = decode[RESPONSE](resultString).getOrElse(throw new IllegalStateException("failed thingy"))
    } yield result
  }

  private def decodeResponse(response: HttpResponse): HttpResponse = {
    val decoder = response.encoding match {
      case HttpEncodings.gzip =>
        Coders.Gzip
      case HttpEncodings.deflate =>
        Coders.Deflate
      case HttpEncodings.identity =>
        Coders.NoCoding
      case other =>
        system.log.warning(s"Unknown encoding [$other], not decoding")
        Coders.NoCoding
    }

    decoder.decodeMessage(response)
  }
}

object AkkaHttpBetfairVendorApi {
  final case class TokenExchangeRequest(
      clientId: ClientId,
      grantType: GrantType,
      code: Code,
      clientSecret: ClientSecret
  )
  final case class RefreshTokenRequest(
      clientId: ClientId,
      grantType: GrantType,
      refreshToken: RefreshToken,
      clientSecret: ClientSecret
  )
}

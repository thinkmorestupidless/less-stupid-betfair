package com.thinkmorestupidless.betfair.auth.impl

import io.circe.Decoder.Result
import io.circe.{Codec, Decoder, Encoder, HCursor, Json}
import com.thinkmorestupidless.betfair.auth.domain.{
  ApplicationKey,
  LoginFailure,
  LoginResponse,
  LoginStatus,
  LoginSuccess,
  SessionToken
}
import com.thinkmorestupidless.betfair.extensions.circe.CirceUtils._

object JsonCodecs {

  implicit val applicationKeyCodec: Codec[ApplicationKey] = bimapString(_.value, ApplicationKey(_))
  implicit val sessionTokenCodec: Codec[SessionToken] = bimapString(_.value, SessionToken(_))

  implicit val loginResponseCodec: Codec[LoginResponse] = Codec.from(
    new Decoder[LoginResponse] {
      override def apply(c: HCursor): Result[LoginResponse] =
        c.downField("loginStatus").as[LoginStatus] match {
          case Right(loginStatus) => fromLoginStatus(loginStatus, c)
          case Left(error)        => throw new IllegalStateException(s"failed to decode login status [$error]")
        }

      def fromLoginStatus(loginStatus: LoginStatus, c: HCursor): Result[LoginResponse] =
        loginStatus match {
          case LoginStatus.Success => c.downField("sessionToken").as[SessionToken].map(LoginSuccess(_))
          case otherLoginStatus    => Right(LoginFailure(otherLoginStatus))
        }
    },
    new Encoder[LoginResponse] {
      override def apply(a: LoginResponse): Json = ???
    }
  )
}

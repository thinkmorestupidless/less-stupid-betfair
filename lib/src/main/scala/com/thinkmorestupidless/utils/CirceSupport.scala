package com.thinkmorestupidless.utils

import org.apache.pekko.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.{ContentTypes, MediaTypes}
import org.apache.pekko.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import org.apache.pekko.util.ByteString
import cats.syntax.either._
import io.circe.jawn._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Printer}

trait CirceSupport {
  private final val compactPrinter: Printer = Printer(
    dropNullValues = true,
    indent = ""
  )

  final implicit def jsonUnmarshaller[T: Decoder]: FromEntityUnmarshaller[T] =
    Unmarshaller.byteArrayUnmarshaller.forContentTypes(ContentTypes.`application/json`).mapWithCharset {
      case (bytes, charset) =>
        if (bytes.length == 0) throw Unmarshaller.NoContentException
        else decode(new String(bytes, charset.nioCharset.name)).valueOr(throw _)
    }

  final implicit def jsonMarshaller[T: Encoder](implicit printer: Printer = compactPrinter): ToEntityMarshaller[T] =
    Marshaller
      .byteStringMarshaller(MediaTypes.`application/json`)
      .compose(obj => ByteString(printer.printToByteBuffer(obj.asJson)))
}

object CirceSupport extends CirceSupport

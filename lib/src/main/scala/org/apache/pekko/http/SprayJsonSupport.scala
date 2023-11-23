package org.apache.pekko.http

import org.apache.pekko.NotUsed
import org.apache.pekko.event.Logging
import org.apache.pekko.http.javadsl.{common, model => jm}
import org.apache.pekko.http.scaladsl.common.EntityStreamingSupport
import org.apache.pekko.http.scaladsl.marshalling._
import org.apache.pekko.http.scaladsl.model.MediaTypes.`application/json`
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshaller.UnsupportedContentTypeException
import org.apache.pekko.http.scaladsl.unmarshalling.{FromByteStringUnmarshaller, FromEntityUnmarshaller, Unmarshaller}
import org.apache.pekko.http.scaladsl.util.FastFuture
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Source}
import org.apache.pekko.util.ByteString
import spray.json._

/** A trait providing automatic to and from JSON marshalling/unmarshalling using an in-scope *spray-json* protocol.
  */
trait SprayJsonSupport {
  implicit def sprayJsonUnmarshallerConverter[T](reader: RootJsonReader[T]): FromEntityUnmarshaller[T] =
    sprayJsonUnmarshaller(reader)

  implicit def sprayJsonUnmarshaller[T](implicit reader: RootJsonReader[T]): FromEntityUnmarshaller[T] =
    sprayJsValueUnmarshaller.map(jsonReader[T].read)
  implicit def sprayJsValueUnmarshaller: FromEntityUnmarshaller[JsValue] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(`application/json`).andThen(sprayJsValueByteStringUnmarshaller)

  implicit def sprayJsValueByteStringUnmarshaller[T]: FromByteStringUnmarshaller[JsValue] =
    Unmarshaller.withMaterializer[ByteString, JsValue](_ =>
      _ => { bs =>
        // .compact so addressing into any address is very fast (also for large chunks)
        // TODO we could optimise ByteStrings to better handle linear access like this (or provide ByteStrings.linearAccessOptimised)
        // TODO IF it's worth it.
        val parserInput = new SprayJsonByteStringParserInput(bs.compact)
        FastFuture.successful(JsonParser(parserInput))
      }
    )
  implicit def sprayJsonByteStringUnmarshaller[T](implicit reader: RootJsonReader[T]): FromByteStringUnmarshaller[T] =
    sprayJsValueByteStringUnmarshaller[T].map(jsonReader[T].read)

  // support for as[Source[T, NotUsed]]
  implicit def sprayJsonSourceReader[T](implicit
      reader: RootJsonReader[T],
      support: EntityStreamingSupport
  ): FromEntityUnmarshaller[Source[T, NotUsed]] =
    Unmarshaller.withMaterializer { implicit ec => implicit mat => e =>
      if (support.supported.matches(e.contentType)) {
        val frames = e.dataBytes.via(support.framingDecoder)
        val unmarshal = sprayJsonByteStringUnmarshaller(reader)(_)
        val unmarshallingFlow =
          if (support.unordered) Flow[ByteString].mapAsyncUnordered(support.parallelism)(unmarshal)
          else Flow[ByteString].mapAsync(support.parallelism)(unmarshal)
        val elements = frames.viaMat(unmarshallingFlow)(Keep.right)
        FastFuture.successful(elements)
      } else FastFuture.failed(UnsupportedContentTypeException(Some(e.contentType), support.supported))
    }

  // #sprayJsonMarshallerConverter
  implicit def sprayJsonMarshallerConverter[T](writer: RootJsonWriter[T])(implicit
      printer: JsonPrinter = CompactPrinter
  ): ToEntityMarshaller[T] =
    sprayJsonMarshaller[T](writer, printer)
  // #sprayJsonMarshallerConverter
  implicit def sprayJsonMarshaller[T](implicit
      writer: RootJsonWriter[T],
      printer: JsonPrinter = CompactPrinter
  ): ToEntityMarshaller[T] =
    sprayJsValueMarshaller.compose(writer.write)
  implicit def sprayJsValueMarshaller(implicit printer: JsonPrinter = CompactPrinter): ToEntityMarshaller[JsValue] =
    Marshaller.StringMarshaller.wrap(MediaTypes.`application/json`)(printer)
}
object SprayJsonSupport extends SprayJsonSupport

/** Entity streaming support, implemented using spray-json.
  *
  * See also <a href="https://github.com/spray/spray-json">github.com/spray/spray-json</a> for details about Spray JSON
  * itself
  */
object SprayJsonEntityStreamingSupport {

  /** Default `application/json` entity streaming support.
    *
    * Provides framing (based on scanning the incoming dataBytes for valid JSON objects, so for example uploads using
    * arrays or new-line separated JSON objects are all parsed correctly) and rendering of Sources as JSON Arrays. A
    * different very popular style of returning streaming JSON is to separate JSON objects on a line-by-line basis, you
    * can configure the support trait to do so by calling `withFramingRendererFlow`.
    *
    * Limits the maximum JSON object length to 8KB, if you want to increase this limit provide a value explicitly.
    *
    * See also <a href="https://en.wikipedia.org/wiki/JSON_Streaming">https://en.wikipedia.org/wiki/JSON_Streaming</a>
    */
  def json(): JsonEntityStreamingSupport = json(8 * 1024)

  /** Default `application/json` entity streaming support.
    *
    * Provides framing (based on scanning the incoming dataBytes for valid JSON objects, so for example uploads using
    * arrays or new-line separated JSON objects are all parsed correctly) and rendering of Sources as JSON Arrays. A
    * different very popular style of returning streaming JSON is to separate JSON objects on a line-by-line basis, you
    * can configure the support trait to do so by calling `withFramingRendererFlow`.
    *
    * See also <a href="https://en.wikipedia.org/wiki/JSON_Streaming">https://en.wikipedia.org/wiki/JSON_Streaming</a>
    */
  def json(maxObjectLength: Int): JsonEntityStreamingSupport = new JsonEntityStreamingSupport(maxObjectLength)

}

final class JsonEntityStreamingSupport(
    maxObjectSize: Int,
    val supported: ContentTypeRange,
    val contentType: ContentType,
    val framingRenderer: Flow[ByteString, ByteString, NotUsed],
    val parallelism: Int,
    val unordered: Boolean
) extends common.JsonEntityStreamingSupport {
  import org.apache.pekko.http.impl.util.JavaMapping.Implicits._

  def this(maxObjectSize: Int) =
    this(
      maxObjectSize,
      ContentTypeRange(ContentTypes.`application/json`),
      ContentTypes.`application/json`,
      Flow[ByteString].intersperse(ByteString("["), ByteString(","), ByteString("]")),
      1,
      false
    )

  override val framingDecoder: Flow[ByteString, ByteString, NotUsed] =
    org.apache.pekko.stream.scaladsl.JsonFraming.objectScanner(maxObjectSize)

  override def withFramingRendererFlow(
      framingRendererFlow: org.apache.pekko.stream.javadsl.Flow[ByteString, ByteString, NotUsed]
  ): JsonEntityStreamingSupport =
    withFramingRenderer(framingRendererFlow.asScala)
  def withFramingRenderer(framingRendererFlow: Flow[ByteString, ByteString, NotUsed]): JsonEntityStreamingSupport =
    new JsonEntityStreamingSupport(maxObjectSize, supported, contentType, framingRendererFlow, parallelism, unordered)

  override def withContentType(ct: jm.ContentType): JsonEntityStreamingSupport =
    new JsonEntityStreamingSupport(maxObjectSize, supported, ct.asScala, framingRenderer, parallelism, unordered)
  override def withSupported(range: jm.ContentTypeRange): JsonEntityStreamingSupport =
    new JsonEntityStreamingSupport(maxObjectSize, range.asScala, contentType, framingRenderer, parallelism, unordered)
  override def withParallelMarshalling(parallelism: Int, unordered: Boolean): JsonEntityStreamingSupport =
    new JsonEntityStreamingSupport(maxObjectSize, supported, contentType, framingRenderer, parallelism, unordered)

  override def toString = s"""${Logging.simpleName(getClass)}($maxObjectSize, $supported, $contentType)"""

}

import org.apache.pekko.annotation.InternalApi
import spray.json.ParserInput.IndexedBytesParserInput

import java.nio.charset.StandardCharsets

/** INTERNAL API
  *
  * ParserInput reading directly off a ByteString. (Based on the ByteArrayBasedParserInput) that avoids a separate
  * decoding step.
  */
@InternalApi
final class SprayJsonByteStringParserInput(bytes: ByteString) extends IndexedBytesParserInput {
  protected def byteAt(offset: Int): Byte = bytes(offset)

  override def length: Int = bytes.size
  override def sliceString(start: Int, end: Int): String =
    bytes.slice(start, end - start).decodeString(StandardCharsets.UTF_8)
  override def sliceCharArray(start: Int, end: Int): Array[Char] =
    StandardCharsets.UTF_8.decode(bytes.slice(start, end).asByteBuffer).array()
}

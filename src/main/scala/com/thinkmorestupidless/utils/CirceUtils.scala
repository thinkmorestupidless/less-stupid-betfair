package com.thinkmorestupidless.utils

import io.circe.{Codec, Decoder, Encoder}

import java.time.Instant

object CirceUtils {

  def bimapString[T](asString: T => String, fromString: String => T): Codec[T] =
    Codec.from(Decoder.decodeString.map(fromString), Encoder.encodeString.contramap[T](asString))

  def bimapInt[T](asInt: T => Int, fromInt: Int => T): Codec[T] =
    Codec.from(Decoder.decodeInt.map(fromInt), Encoder.encodeInt.contramap[T](asInt))

  def bimapLong[T](asLong: T => Long, fromLong: Long => T): Codec[T] =
    Codec.from(Decoder.decodeLong.map(fromLong), Encoder.encodeLong.contramap[T](asLong))

  def bimapBoolean[T](asBoolean: T => Boolean, fromBoolean: Boolean => T): Codec[T] =
    Codec.from(Decoder.decodeBoolean.map(fromBoolean), Encoder.encodeBoolean.contramap[T](asBoolean))

  def bimapInstant[T](asInstant: T => Instant, fromInstant: Instant => T): Codec[T] =
    Codec.from(Decoder.decodeInstant.map(fromInstant), Encoder.encodeInstant.contramap[T](asInstant))

  def bimapDecimal[T](asDecimal: T => BigDecimal, fromDecimal: BigDecimal => T): Codec[T] =
    Codec.from(Decoder.decodeBigDecimal.map(fromDecimal), Encoder.encodeBigDecimal.contramap[T](asDecimal))

  def bimapStringMap[T](asMap: T => Map[String, String], fromMap: Map[String, String] => T): Codec[T] =
    Codec.from(Decoder.decodeMap[String, String].map(fromMap), Encoder.encodeMap[String, String].contramap[T](asMap))
}

package com.thinkmorestupidless.utils

import spray.json._

object FileSupport {

  def stringFromResource(fileName: String): String =
    scala.io.Source.fromResource(s"$fileName").mkString

  def jsonFromResource(fileName: String): JsValue =
    stringFromResource(fileName).parseJson
}

import sbt._

object DependencyVersions {
  val akkaVersion = "2.7.0"
  val akkaHttpVersion = "10.4.0"
  val akkaHttpJsonVersion = "1.39.2"
  val logbackVersion = "1.2.3"
  val circeVersion = "0.14.1"
  val enumeratumVersion = "1.6.1"
  val pureConfigVersion = "0.17.1"
  val sbtioVersion = "1.7.0"
  val scalaVersion = "2.13.9"
  val scalaTestVersion = "3.1.0"
}

object Dependencies {
  import DependencyVersions._

  private val akka = Seq(
    "com.typesafe.akka" %% "akka-stream-typed"
  ).map(_ % akkaVersion)

  private val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http"
  ).map(_ % akkaHttpVersion)

  private val akkaHttpJson = Seq(
    "de.heikoseeberger" %% "akka-http-circe"
  ).map(_ % akkaHttpJsonVersion)

  private val akkaTesting = Seq(
    "com.typesafe.akka" %% "akka-stream-testkit"
  ).map(_ % akkaVersion % Test)

  private val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion) ++ Seq(
    "com.beachape" %% "enumeratum-circe"
  ).map(_ % enumeratumVersion)

  private val enumeratum = Seq(
    "com.beachape" %% "enumeratum"
  ).map(_ % enumeratumVersion)

  private val pureConfig = Seq(
    "com.github.pureconfig" %% "pureconfig"
  ).map(_ % pureConfigVersion)

  private val sbtio = Seq(
    "org.scala-sbt" %% "io"
  ).map(_ % sbtioVersion)

  private val scalatest = Seq(
    "org.scalatest" %% "scalatest"
  ).map(_ % scalaTestVersion % Test)

  val production = akka ++ akkaHttp ++ akkaHttpJson ++ circe ++ enumeratum ++ pureConfig ++ sbtio
  val test = akkaTesting ++ scalatest

  val all = production ++ test
}

import sbt._

object DependencyVersions {
  val akkaVersion = "2.7.0"
  val akkaHttpVersion = "10.4.0"
  val akkaHttpJsonVersion = "1.39.2"
  val logbackVersion = "1.4.11"
  val circeVersion = "0.14.1"
  val enumeratumVersion = "1.6.1"
  val kebsVersion = "1.8.1"
  val pekkoVersion = "1.0.1"
  val pekkoHttpVersion = "1.0.0"
  val playWsVersion = "3.0.0"
  val postgresVersion = "42.1.4"
  val pureConfigVersion = "0.17.1"
  val sbtioVersion = "1.7.0"
  val scalaVersion = "2.13.9"
  val scalaTestVersion = "3.1.0"
  val slf4jVersion = "1.7.30"
  val slickVersion = "3.3.3"
  val slickPgVersion = "0.19.3"
  val slickMigrationApiVersion = "0.7.0"
  val sprayVersion = "1.3.6"
  val testContainersVersion = "1.17.5"
  val wiremockVersion = "2.35.0"
}

object Dependencies {
  import DependencyVersions._

//  private val akka = Seq(
//    "com.typesafe.akka" %% "akka-actor-testkit-typed",
//    "com.typesafe.akka" %% "akka-stream-typed"
//  ).map(_ % akkaVersion)
//
//  private val akkaHttp = Seq(
//    "com.typesafe.akka" %% "akka-http"
//  ).map(_ % akkaHttpVersion)
//
//  private val akkaHttpJson = Seq(
//    "de.heikoseeberger" %% "akka-http-circe"
//  ).map(_ % akkaHttpJsonVersion)
//
//  private val akkaTesting = Seq(
//    "com.typesafe.akka" %% "akka-stream-testkit"
//  ).map(_ % akkaVersion % Test)

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

  private val kebs = Seq(
    "pl.iterators" %% "kebs-spray-json"
  ).map(_ % kebsVersion)

  private val logging = Seq(
    "ch.qos.logback" % "logback-classic"
  ).map(_ % logbackVersion)

  private val pekko = Seq(
    "org.apache.pekko" %% "pekko-actor-typed",
    "org.apache.pekko" %% "pekko-persistence-typed",
    "org.apache.pekko" %% "pekko-cluster-sharding-typed",
    "org.apache.pekko" %% "pekko-stream",
    "org.apache.pekko" %% "pekko-testkit"
  ).map(_ % pekkoVersion)

  private val pekkoHttp = Seq(
    "org.apache.pekko" %% "pekko-http"
  ).map(_ % pekkoHttpVersion)

  private val playWs = Seq(
    "org.playframework" %% "play-ahc-ws-standalone",
    "org.playframework" %% "play-ws-standalone-json"
  ).map(_ % playWsVersion)

  private val postgres = Seq(
    "org.postgresql" % "postgresql"
  ).map(_ % postgresVersion)

  private val pureConfig = Seq(
    "com.github.pureconfig" %% "pureconfig"
  ).map(_ % pureConfigVersion)

  private val sbtio = Seq(
    "org.scala-sbt" %% "io"
  ).map(_ % sbtioVersion)

  private val scalatest = Seq(
    "org.scalatest" %% "scalatest"
  ).map(_ % scalaTestVersion % Test)

  private val slick = Seq(
    "com.typesafe.slick" %% "slick-hikaricp"
  ).map(_ % slickVersion) ++ Seq(
    "com.github.tminglei" %% "slick-pg",
    "com.github.tminglei" %% "slick-pg_circe-json"
  ).map(_ % slickPgVersion) ++ Seq(
    "io.github.nafg.slick-migration-api" %% "slick-migration-api-flyway"
  ).map(_ % slickMigrationApiVersion)

  private val spray = Seq(
    "io.spray" %% "spray-json"
  ).map(_ % sprayVersion)

  private val testcontainers = Seq(
    "org.testcontainers" % "postgresql"
  ).map(_ % testContainersVersion)

  private val wiremock = Seq(
    "com.github.tomakehurst" % "wiremock-jre8"
  ).map(_ % wiremockVersion % Test)

  val libDependencies = {
    val production =
//      akka ++
//        akkaHttp ++
//        akkaHttpJson ++
        circe ++
        logging ++
        enumeratum ++
        kebs ++
        pekko ++
        pekkoHttp ++
        playWs ++
        postgres ++
        pureConfig ++
        sbtio ++
        slick ++
        spray

    val test =
//      akkaTesting ++
        scalatest ++
        testcontainers ++
        wiremock

    production ++ test
  }

  val websocketExampleDependencies =
    logging

  val grpcExampleDependencies =
    logging ++
    pekkoHttp

  private val gentePekko = Seq(
    "org.apache.pekko" %% "pekko-persistence-typed",
    "org.apache.pekko" %% "pekko-cluster-sharding-typed",
    "org.apache.pekko" %% "pekko-stream"
  ).map(_ % pekkoVersion)

  private val gentePekkoHttp = Seq(
    "org.apache.pekko" %% "pekko-http"
  ).map(_ % pekkoHttpVersion)

  private val gentePekkoTest = Seq(
    "org.apache.pekko" %% "pekko-persistence-testkit",
    "org.apache.pekko" %% "pekko-stream-testkit"
  ).map(_ % pekkoVersion % Test)

  val genteDependencies =
    gentePekko ++
    gentePekkoTest

  val genteExampleGrpcDependencies =
    gentePekkoHttp

}

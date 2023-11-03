import ReleaseTransformations.*
import com.jsuereth.sbtpgp.SbtPgp.autoImport.PgpKeys
import sbtrelease.ReleasePlugin.autoImport.releasePublishArtifactsAction

import scala.collection.immutable.Seq

ThisBuild / organization := "com.thinkmorestupidless"
ThisBuild / dynverSeparator := "-"
ThisBuild / scalaVersion := DependencyVersions.scalaVersion
ThisBuild / scalafixDependencies := Dependencies.scalaFixPlugins

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:_",
    "-Xlog-reflective-calls",
    "-Xlint:-byname-implicit",
    "-Ybackend-parallelism",
    "8",
    "-Ywarn-dead-code",
    "-Wunused",
    "-unchecked"
  ),
  scalafmtOnCompile := true
)

lazy val root = project.in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.topLevelDependencies
  )
  .aggregate(
    lib,
    `example-streams-api`,
    `example-streams-api-postgres`,
    `example-websocket`,
    `example-grpc`,
    `example-multiple-channels`
  )
  .dependsOn(Array(
    lib,
    `example-streams-api`,
    `example-streams-api-postgres`,
    `example-websocket`,
    `example-grpc`,
    `example-multiple-channels`).map(_ % "test->test"): _*)

lazy val lib = project.in(file("lib"))
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(protobufs)
  .settings(commonSettings)
  .settings(
    name := "less-stupid-betfair",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.thinkmorestupidless",
    libraryDependencies ++= Dependencies.libDependencies,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseCrossBuild := false,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("publishSigned"),
      releaseStepCommand("sonatypeBundleRelease"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )

lazy val `example-streams-api` = project.in(file("examples/streams-api"))
  .dependsOn(lib)
  .settings(commonSettings)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.streamsApiExampleDependencies,
    run / fork := true
  )

lazy val `example-streams-api-postgres` = project.in(file("examples/streams-api-postgres"))
  .dependsOn(lib)
  .settings(commonSettings)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.streamsApiPostgresExampleDependencies,
    run / fork := true
  )

lazy val `example-websocket` = project.in(file("examples/websocket"))
  .dependsOn(lib)
  .settings(commonSettings)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.websocketExampleDependencies,
    run / fork := true
  )

lazy val `example-grpc` = project.in(file("examples/grpc"))
  .enablePlugins(PekkoGrpcPlugin)
  .dependsOn(lib)
  .settings(commonSettings)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.grpcExampleDependencies,
    run / fork := true
  )

lazy val `example-multiple-channels` = project.in(file("examples/multiple-channels"))
  .dependsOn(lib)
  .settings(commonSettings)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.genteExampleGrpcDependencies,
    run / fork := true
  )

lazy val protobufs = project.in(file("protobufs"))
  .enablePlugins(PekkoGrpcPlugin)

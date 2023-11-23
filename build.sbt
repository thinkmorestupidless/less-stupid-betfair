import ReleaseTransformations.*
import com.jsuereth.sbtpgp.SbtPgp.autoImport.PgpKeys
import sbtrelease.ReleasePlugin.autoImport.releasePublishArtifactsAction

ThisBuild / organization := "com.thinkmorestupidless"
ThisBuild / dynverSeparator := "-"
ThisBuild / scalaVersion := DependencyVersions.scalaVersion

lazy val root = project.in(file("."))
  .enablePlugins(BuildInfoPlugin)
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
    gente
  )
  .dependsOn(Array(
    lib,
    `example-streams-api`,
    `example-streams-api-postgres`,
    `example-websocket`,
    `example-grpc`).map(_ % "test->test"): _*)

lazy val lib = project.in(file("lib"))
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(protobufs)
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
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.streamsApiExampleDependencies,
    run / fork := true
  )

lazy val `example-streams-api-postgres` = project.in(file("examples/streams-api-postgres"))
  .dependsOn(lib)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.streamsApiPostgresExampleDependencies,
    run / fork := true
  )

lazy val `example-websocket` = project.in(file("examples/websocket"))
  .dependsOn(lib)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.websocketExampleDependencies,
    run / fork := true
  )

lazy val `example-grpc` = project.in(file("examples/grpc"))
  .enablePlugins(PekkoGrpcPlugin)
  .dependsOn(lib)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.grpcExampleDependencies,
    run / fork := true
  )

lazy val gente = project.in(file("gente"))
  .dependsOn(lib)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.genteDependencies,
    run / fork := true
  )

lazy val `gente-example-grpc` = project.in(file("gente-example-grpc"))
  .dependsOn(gente)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.genteExampleGrpcDependencies,
    run / fork := true
  )

lazy val protobufs = project.in(file("protobufs"))
  .enablePlugins(PekkoGrpcPlugin)

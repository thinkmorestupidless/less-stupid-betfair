import ReleaseTransformations.*
import com.jsuereth.sbtpgp.SbtPgp.autoImport.PgpKeys
import sbtrelease.ReleasePlugin.autoImport.releasePublishArtifactsAction

ThisBuild / organization := "com.thinkmorestupidless"
ThisBuild / dynverSeparator := "-"
ThisBuild / scalaVersion := DependencyVersions.scalaVersion

lazy val root = project.in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    publish := false
  )
  .aggregate(
    lib
  )

lazy val lib = project.in(file("lib"))
  .enablePlugins(BuildInfoPlugin)
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

lazy val `example-websocket` = project.in(file("examples/websocket"))
  .dependsOn(lib)
  .settings(
    publish := false,
    libraryDependencies ++= Dependencies.websocketExampleDependencies,
    run / fork := true
  )

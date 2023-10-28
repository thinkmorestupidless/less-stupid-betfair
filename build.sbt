import ReleaseTransformations._

ThisBuild / name := "less-stupid-betfair"
ThisBuild / organization := "com.thinkmorestupidless"
ThisBuild / scalaVersion := "2.13.11"
ThisBuild / dynverSeparator := "-"
ThisBuild / scalaVersion := DependencyVersions.scalaVersion

libraryDependencies ++= Dependencies.all

releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseCrossBuild := false
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  // For non cross-build projects, use releaseStepCommand("publishSigned")
  releaseStepCommandAndRemaining("publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

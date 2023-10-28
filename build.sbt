import ReleaseTransformations._

organization := "com.thinkmorestupidless"
scalaVersion := "2.13.11"
scmInfo := Some(ScmInfo(url("https://github.com/thinkmorestupidless/less-stupid-betfair"), "https://github.com/thinkmorestupidless/less-stupid-betfair.git"))
developers := List(Developer("thinkmorestupidless", "thinkmorestupidless", "trevor@thinkmorestupidless.com", url("https://github.com/thinkmorestupidless")))
licenses += ("MIT", url("https://opensource.org/license/mit/"))

ThisBuild / dynverSeparator := "-"

sonatypeCredentialHost := "s01.oss.sonatype.org"

//credentials += Credentials(Path.userHome / ".sbt" / "1.0" / "sonatype.sbt")

scalaVersion := DependencyVersions.scalaVersion

libraryDependencies ++= Dependencies.all

publishTo := sonatypePublishToBundle.value

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

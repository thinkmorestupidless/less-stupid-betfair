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

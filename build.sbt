ThisBuild / organization := "com.thinkmorestupidless"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.11"
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/thinkmorestupidless/less-stupid-betfair"), "https://github.com/thinkmorestupidless/less-stupid-betfair.git"))
ThisBuild / developers := List(Developer("thinkmorestupidless", "thinkmorestupidless", "trevor@thinkmorestupidless.com", url("https://github.com/thinkmorestupidless")))
ThisBuild / licenses += ("MIT", url("https://opensource.org/license/mit/"))
ThisBuild / publishMavenStyle := true

ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")

scalaVersion := DependencyVersions.scalaVersion

libraryDependencies ++= Dependencies.all

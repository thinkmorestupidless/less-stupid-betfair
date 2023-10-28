import xerial.sbt.Sonatype._

publishMavenStyle := true

sonatypeProfileName    := "com.thinkmorestupidless"
sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeProjectHosting := Some(GitHubHosting(user = "thinkmorestupidless", repository = "less-stupid-betfair", email = "trevor@thinkmorestupidless.com"))
developers := List(
  Developer(id = "trevor", name = "Trevor Burton-McCreadie", email = "trevor@thinkmorestupidless.com", url = url("https://thinkmorestupidless.com"))
)
scmInfo := Some(ScmInfo(url("https://github.com/thinkmorestupidless/less-stupid-betfair"), "https://github.com/thinkmorestupidless/less-stupid-betfair.git"))
licenses += ("MIT", url("https://opensource.org/license/mit/"))

publishTo := sonatypePublishToBundle.value

package com.thinkmorestupidless.betfair.auth.impl

import com.thinkmorestupidless.betfair.auth.domain.SessionToken
import com.thinkmorestupidless.betfair.auth.impl.JsonCodecs.storedSessionCodec
import com.thinkmorestupidless.betfair.auth.impl.SessionTokenStore.StoredSession
import com.thinkmorestupidless.betfair.core.impl.FileProviderConfig
import io.circe.parser.decode
import io.circe.syntax._
import sbt.io.syntax._
import sbt.io.{IO => sbtio}

import java.time.Clock
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

final class FileSessionTokenStore private (sessionFile: File)(implicit ec: ExecutionContext, clock: Clock)
    extends SessionTokenStore {

  override def read(): Future[Option[SessionToken]] = Future {
    if (sessionFile.exists) {
      val json = sbtio.read(sessionFile)
      val stored = decode[StoredSession](json)
        .getOrElse(throw new IllegalStateException(s"session file [$sessionFile] does not contain valid JSON"))

      val age = ChronoUnit.HOURS.between(stored.createdAt, clock.instant())

      if (age < 24)
        Some(stored.sessionToken)
      else
        None

    } else {
      None
    }
  }

  override def write(sessionToken: SessionToken): Unit = {
    val stored = StoredSession(sessionToken, clock.instant())
    val json = stored.asJson.noSpaces

    sbtio.write(sessionFile, json)
  }
}

object FileSessionTokenStore {

  def apply(config: FileProviderConfig)(implicit ec: ExecutionContext, clock: Clock): SessionTokenStore = {
    val sessionFile = file(config.filePath.value)
    new FileSessionTokenStore(sessionFile)
  }
}

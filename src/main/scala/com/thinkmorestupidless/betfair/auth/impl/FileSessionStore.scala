package com.thinkmorestupidless.betfair.auth.impl

import com.thinkmorestupidless.betfair.auth.domain.{BetfairSession, SessionStore}
import com.thinkmorestupidless.betfair.auth.impl.FileSessionStore.{StoredSession, _}
import com.thinkmorestupidless.betfair.auth.impl.JsonCodecs.{applicationKeyCodec, sessionTokenCodec}
import io.circe.Codec
import io.circe.generic.semiauto._
import io.circe.parser.decode
import io.circe.syntax._
import sbt.io.syntax._
import sbt.io.{IO => sbtio}

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}

class FileSessionStore()(implicit ec: ExecutionContext, clock: Clock) extends SessionStore {

  override def read(): Future[Option[BetfairSession]] = Future {
    val file = SessionFile

    if (file.exists) {
      val json = sbtio.read(file)
      val stored = decode[StoredSession](json)
        .getOrElse(throw new IllegalStateException(s"session file [$file] does not contain valid JSON"))

      val age = ChronoUnit.HOURS.between(stored.createdAt, clock.instant())

      if (age < 24)
        Some(stored.session)
      else
        None

    } else {
      None
    }
  }

  override def write(session: BetfairSession) = {
    val stored = StoredSession(session, clock.instant())
    val json = stored.asJson.noSpaces

    sbtio.write(SessionFile, json)
  }
}

object FileSessionStore {
  val SessionFile = file("./session")

//  def betfairHomeDir() =
//    sys.env.get()

  final case class StoredSession(session: BetfairSession, createdAt: Instant)

  implicit val betfairSessionCodec: Codec[BetfairSession] = deriveCodec[BetfairSession]
  implicit lazy val storedSessionCodec: Codec[StoredSession] = deriveCodec[StoredSession]
}

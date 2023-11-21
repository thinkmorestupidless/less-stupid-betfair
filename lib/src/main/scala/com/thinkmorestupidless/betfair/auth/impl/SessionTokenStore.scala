package com.thinkmorestupidless.betfair.auth.impl

import com.thinkmorestupidless.betfair.auth.domain.SessionToken
import com.thinkmorestupidless.betfair.core.impl.{SessionStoreConfig, SessionStoreProviderType}

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}

trait SessionTokenStore {

  def read(): Future[Option[SessionToken]]

  def write(session: SessionToken): Unit
}

object SessionTokenStore {

  final case class StoredSession(sessionToken: SessionToken, createdAt: Instant)

  def fromConfig(config: SessionStoreConfig)(implicit ec: ExecutionContext, clock: Clock): SessionTokenStore =
    config.providerType match {
      case SessionStoreProviderType.InMem => InMemorySessionTokenStore
      case SessionStoreProviderType.File  => FileSessionTokenStore(config.fileProvider)
    }
}

package gente.oss.betfair.channels.impl.tokens

import com.thinkmorestupidless.betfair.vendor.domain.AccessToken
import gente.oss.betfair.channels.impl.tokens.CustomerTokenProtocol.{
  Command,
  GetToken,
  TokenNotInitialized,
  UpdateToken
}
import org.apache.pekko.Done
import org.apache.pekko.actor.Status
import org.apache.pekko.persistence.typed.state.scaladsl.Effect

object CustomerTokenCommandHandler {

  def apply(): (Option[AccessToken], Command) => Effect[Option[AccessToken]] =
    (state, command) =>
      (state, command) match {
        case (_, UpdateToken(newToken, replyTo))    => Effect.persist(Some(newToken)).thenReply(replyTo)(_ => Done)
        case (Some(accessToken), GetToken(replyTo)) => Effect.reply(replyTo)(Status.Success(accessToken))
        case (None, GetToken(replyTo))              => Effect.reply(replyTo)(Status.Failure(TokenNotInitialized()))
      }
}

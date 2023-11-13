package gente.oss.betfair.channels.impl.tokens

import com.thinkmorestupidless.betfair.vendor.domain.AccessToken
import org.apache.pekko.Done
import org.apache.pekko.actor.Status.Status
import org.apache.pekko.actor.typed.ActorRef

object CustomerTokenProtocol {

  sealed trait Command
  final case class UpdateToken(accessToken: AccessToken, replyTo: ActorRef[Done]) extends Command
  final case class GetToken(replyTo: ActorRef[Status]) extends Command

  final case class TokenNotInitialized() extends Throwable
}

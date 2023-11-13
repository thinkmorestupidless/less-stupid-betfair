package gente.oss.betfair.channels.impl.tokens

import com.thinkmorestupidless.betfair.vendor.domain.AccessToken
import gente.oss.betfair.channels.domain.{CustomerId, CustomerTokensService}
import gente.oss.betfair.channels.impl.tokens.CustomerTokenProtocol.{Command, GetToken, UpdateToken}
import org.apache.pekko.Done
import org.apache.pekko.actor.Status.{Failure, Status, Success}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import org.apache.pekko.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

final class ShardedDurableStateCustomerTokensService()(implicit system: ActorSystem[_]) extends CustomerTokensService {

  private val sharding = ClusterSharding(system)

  private implicit val ec: ExecutionContext = system.executionContext
  private implicit val timeout: Timeout = Timeout(2.seconds)

  override def updateTokenForCustomer(customerId: CustomerId, accessToken: AccessToken): Future[Done] =
    entityRefFor(customerId).ask(replyTo => UpdateToken(accessToken, replyTo))

  override def getTokenForCustomer(customerId: CustomerId): Future[Option[AccessToken]] =
    entityRefFor(customerId).ask[Status](replyTo => GetToken(replyTo)).map {
      case Success(accessToken: AccessToken) => Some(accessToken)
      case Success(_)                        => None
      case Failure(cause)                    => None
    }

  private def entityRefFor(customerId: CustomerId): EntityRef[Command] =
    sharding.entityRefFor(CustomerTokenShardRegion.TypeKey, customerId.value)
}

object ShardedDurableStateCustomerTokensService {

  def apply()(implicit system: ActorSystem[_]): CustomerTokensService = {
    CustomerTokenShardRegion.init()
    new ShardedDurableStateCustomerTokensService()
  }
}

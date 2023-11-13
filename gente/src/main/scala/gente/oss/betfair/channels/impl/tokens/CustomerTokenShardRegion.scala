package gente.oss.betfair.channels.impl.tokens

import gente.oss.betfair.channels.impl.tokens.CustomerTokenProtocol.Command
import gente.oss.betfair.channels.impl.tokens.{CustomerTokenBehaviour, CustomerTokenCommandHandler}
import org.apache.pekko.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import org.apache.pekko.persistence.typed.PersistenceId

object CustomerTokenShardRegion {

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Tokens")

  def init()(implicit system: ActorSystem[_]): Unit = {
    val entity = Entity(TypeKey) { entityContext =>
      CustomerTokenShardRegion(PersistenceId.ofUniqueId(entityContext.entityId))
    }
    ClusterSharding(system).init(entity)
  }

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      Behaviors
        .supervise(CustomerTokenBehaviour(persistenceId))
        .onFailure[IllegalStateException](SupervisorStrategy.resume)
    }
}

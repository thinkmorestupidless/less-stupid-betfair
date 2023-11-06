package gente.oss.betfair.channels.impl.channels

import gente.oss.betfair.channels.impl.channels.ChannelProtocol.Command
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import org.apache.pekko.persistence.typed.PersistenceId

object ChannelShardRegion {

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Channel")

  def init()(implicit system: ActorSystem[_]): Unit = {
    val entity = Entity(TypeKey) { entityContext =>
      ChannelShardRegion(PersistenceId.ofUniqueId(entityContext.entityId))
    }
    ClusterSharding(system).init(entity)
  }

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      Behaviors.supervise(ChannelBehaviour(persistenceId)).onFailure[IllegalStateException](SupervisorStrategy.resume)
    }
}

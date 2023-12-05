package com.thinkmorestupidless.betfair.streams.marketdefinitions.impl.cluster

import com.thinkmorestupidless.betfair.streams.marketdefinitions.impl.cluster.MarketDefinitionProtocol.Command
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import org.apache.pekko.persistence.typed.PersistenceId

object MarketDefinitionShardRegion {

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Channel")

  def init()(implicit system: ActorSystem[_]): Unit = {
    val entity = Entity(TypeKey) { entityContext =>
      MarketDefinitionShardRegion(PersistenceId.ofUniqueId(entityContext.entityId))
    }
    ClusterSharding(system).init(entity)
  }

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    Behaviors.setup[Command] { _ =>
      Behaviors
        .supervise(MarketDefinitionBehaviour(persistenceId))
        .onFailure[IllegalStateException](SupervisorStrategy.resume)
    }
}

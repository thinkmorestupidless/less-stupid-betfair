package com.thinkmorestupidless.betfair.streams.impl

package less.stupid.betting.betfair.socket.impl

import akka.actor.typed.ActorSystem
import io.circe.Json
import com.thinkmorestupidless.betfair.streams.domain.{
  GlobalMarketFilterRepository,
  MarketFilter,
  SocketChannelId,
  SocketChannelMarketFilterRepository
}
import com.thinkmorestupidless.betfair.extensions.slick.CustomPostgresProfile.api._
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import io.circe.syntax._
import com.thinkmorestupidless.betfair.streams.impl.JsonCodecs._
import com.thinkmorestupidless.betfair.streams.impl.SlickMarketFilterRepository.{
  SocketChannelMarketFilterRow,
  SocketChannelMarketFilterTable
}
import com.thinkmorestupidless.betfair.streams.impl.SlickMarketFilterRepositoryMappers._
import com.thinkmorestupidless.betfair.streams.impl.MarketFilterUtils._

import scala.concurrent.{ExecutionContext, Future}

final class SlickMarketFilterRepository(dbConfig: DatabaseConfig[JdbcProfile])(implicit
                                                                               ec: ExecutionContext
) extends SocketChannelMarketFilterRepository
  with GlobalMarketFilterRepository {
  private val db = dbConfig.db

  private val marketFilters: TableQuery[SocketChannelMarketFilterTable] = TableQuery[SocketChannelMarketFilterTable]

  override def upsertMarketFilterForChannelId(channelId: SocketChannelId, marketFilter: MarketFilter): Future[Unit] =
    getOrElse(channelId, marketFilter).map(_ => ())

  override def getCurrentFilterForChannelId(channelId: SocketChannelId): Future[MarketFilter] =
    getOrElse(channelId, MarketFilter.empty)

  override def getAllFiltersForAllChannelIds(): Future[Map[SocketChannelId, MarketFilter]] =
    db.run(marketFilters.result)
      .map(
        _.foldLeft(Map.empty[SocketChannelId, MarketFilter])((acc, next) => acc + (next.channelId -> next.marketFilter))
      )

  override def upsertGlobalMarketFilter(marketFilter: MarketFilter): Future[Unit] =
    upsertMarketFilterForChannelId(GlobalMarketFilterRepository.GlobalChannelId, marketFilter)

  override def getCurrentGlobalFilter(): Future[MarketFilter] =
    getOrElse(GlobalMarketFilterRepository.GlobalChannelId, MarketFilter.empty)

  private def getOrElse(channelId: SocketChannelId, marketFilter: MarketFilter): Future[MarketFilter] = {
    val query = for {
      maybeExisting <- marketFilters.filter(_.channelId === channelId).result.headOption
      newOrMerged = maybeExisting.fold(SocketChannelMarketFilterRow(channelId, marketFilter))(existing =>
        existing.copy(marketFilter = existing.marketFilter.mergeWith(marketFilter))
      )
      _ <- marketFilters.insertOrUpdate(newOrMerged)
    } yield newOrMerged.marketFilter

    db.run(query)
  }
}

object SlickMarketFilterRepository {

  def apply()(implicit system: ActorSystem[_]): SlickMarketFilterRepository = {
    val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("slick", system.settings.config)
    new SlickMarketFilterRepository(dbConfig)(system.executionContext)
  }

  private final case class SocketChannelMarketFilterRow(channelId: SocketChannelId, marketFilter: MarketFilter)

  private class SocketChannelMarketFilterTable(tag: Tag)
    extends Table[SocketChannelMarketFilterRow](tag, "betfair_market_filters") {
    def channelId = column[SocketChannelId]("channel_id", O.PrimaryKey, O.AutoInc)
    def filter = column[MarketFilter]("filter")
    def * = (channelId, filter).mapTo[SocketChannelMarketFilterRow]
  }
}

private object SlickMarketFilterRepositoryMappers {

  implicit val socketChannelIdMapper: BaseColumnType[SocketChannelId] =
    MappedColumnType.base[SocketChannelId, String](_.value, SocketChannelId(_))

  implicit val marketFilterMapper: BaseColumnType[MarketFilter] =
    MappedColumnType.base[MarketFilter, Json](
      _.asJson,
      json =>
        json
          .as[MarketFilter]
          .getOrElse(
            throw new IllegalArgumentException(s"cannot decode MarketFilter from database row content '$json'")
          )
    )
}

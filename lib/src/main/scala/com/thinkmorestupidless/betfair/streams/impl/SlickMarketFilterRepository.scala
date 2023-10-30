package com.thinkmorestupidless.betfair.streams.impl

package less.stupid.betting.betfair.socket.impl

import akka.actor.typed.ActorSystem
import com.thinkmorestupidless.betfair.streams.domain.{GlobalMarketFilterRepository, MarketFilter}
import com.thinkmorestupidless.betfair.streams.impl.JsonCodecs._
import com.thinkmorestupidless.betfair.streams.impl.MarketFilterUtils._
import com.thinkmorestupidless.betfair.streams.impl.less.stupid.betting.betfair.socket.impl.SlickMarketFilterRepository.{SocketChannelMarketFilterRow, SocketChannelMarketFilterTable}
import com.thinkmorestupidless.extensions.slick.CustomPostgresProfile.api._
import io.circe.Json
import io.circe.syntax._
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

final class SlickMarketFilterRepository(dbConfig: DatabaseConfig[JdbcProfile])(implicit
                                                                               ec: ExecutionContext
) extends GlobalMarketFilterRepository {
  private val db = dbConfig.db

  private val marketFilters: TableQuery[SocketChannelMarketFilterTable] = TableQuery[SocketChannelMarketFilterTable]

  override def upsertGlobalMarketFilter(marketFilter: MarketFilter): Future[Unit] = {
    val query = for {
      maybeExisting <- marketFilters.filter(_.id === SlickMarketFilterRepository.Id).result.headOption
      newOrMerged = maybeExisting.fold(SocketChannelMarketFilterRow(SlickMarketFilterRepository.Id, marketFilter))(existing =>
        existing.copy(marketFilter = existing.marketFilter.mergeWith(marketFilter))
      )
      _ <- marketFilters.insertOrUpdate(newOrMerged)
    } yield newOrMerged.marketFilter

    db.run(query).map(_ => ())
  }

  override def getCurrentGlobalFilter(): Future[MarketFilter] =
    db.run(marketFilters.filter(_.id === SlickMarketFilterRepository.Id).result.headOption.map(_.map(_.marketFilter).getOrElse(MarketFilter.empty)))
}

object SlickMarketFilterRepository {
  import SlickMarketFilterRepositoryMappers._

  private val Id = "GLOBAL"

  def apply()(implicit system: ActorSystem[_]): SlickMarketFilterRepository = {
    val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("slick", system.settings.config)
    new SlickMarketFilterRepository(dbConfig)(system.executionContext)
  }

  private final case class SocketChannelMarketFilterRow(id: String, marketFilter: MarketFilter)

  private class SocketChannelMarketFilterTable(tag: Tag)
    extends Table[SocketChannelMarketFilterRow](tag, "global_market_filter") {
    def id = column[String]("id", O.PrimaryKey, O.AutoInc)
    def filter = column[MarketFilter]("market_filter")
    def * = (id, filter).mapTo[SocketChannelMarketFilterRow]
  }
}

private object SlickMarketFilterRepositoryMappers {

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

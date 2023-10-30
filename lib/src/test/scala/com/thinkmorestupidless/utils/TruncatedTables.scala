package com.thinkmorestupidless.utils

import com.thinkmorestupidless.extensions.slick.CustomPostgresProfile.api._
import com.thinkmorestupidless.utils.FutureSupport.await
import com.thinkmorestupidless.utils.TruncatedTables.truncateTables
import org.scalatest.BeforeAndAfterEach
import slick.basic.DatabaseConfig
import slick.jdbc.SetParameter.SetUnit
import slick.jdbc.{JdbcProfile, SQLActionBuilder}

import scala.concurrent.ExecutionContext

trait TruncatedTables { self: IntegrationSpec with BeforeAndAfterEach =>

  override def afterEach(): Unit =
    truncateTables()
}

object TruncatedTables {
  val excludedTables = Set("flyway_schema_history")

  def truncateTables()(implicit ec: ExecutionContext, dbConfig: DatabaseConfig[JdbcProfile]): Unit =
    await(for {
      unfilteredTableNames <-
        dbConfig.db.run(sql"""SELECT tablename FROM pg_tables WHERE schemaname = current_schema()""".as[String])
      filteredTableNames = unfilteredTableNames.filter(tableName => !excludedTables.contains(tableName))
      truncates =
        filteredTableNames.map(table => SQLActionBuilder(List(s"TRUNCATE TABLE $table CASCADE"), SetUnit).asUpdate)
      _ <- dbConfig.db.run(DBIO.sequence(truncates))
    } yield ())

  def withTruncatedTables(test: => Any)(implicit ec: ExecutionContext, dbConfig: DatabaseConfig[JdbcProfile]): Unit = {
    truncateTables()
    test
  }
}

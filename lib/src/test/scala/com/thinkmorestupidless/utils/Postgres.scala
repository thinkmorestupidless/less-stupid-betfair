package com.thinkmorestupidless.utils

import org.flywaydb.core.Flyway
import org.rnorth.ducttape.unreliables.Unreliables
import org.testcontainers.containers.PostgreSQLContainer
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.migration.api.flyway.DatabaseDatasource

import java.time.Clock

object Postgres {
  lazy val instance: Postgres = {
    val container = new Postgres()
    container.start()
    container
  }
}

class Postgres extends PostgreSQLContainer[Postgres]("postgres:12.7") with FutureSupport with RandomUtils {
  private val clock: Clock = Clock.systemUTC()

  withUsername("betfair_test")
  withPassword("e5ERPU6F5pPh")
  withCommand("postgres -c max_connections=500")
  withReuse(true)

  def initNewDatabase(): String = {
    val databaseName = s"betfair_test_${generateRandomString(clock)}"
    createDatabase(databaseName)
    // for some reason, sometimes slick fails with `java.sql.SQLException: No suitable driver`
    // this seems to only happen during the first run against the database
    // it should be fixed in a better way, but for now - a few retries should do...
    Unreliables.retryUntilSuccess(10, () => runMigrations(databaseName))

    databaseName
  }

  private def createDatabase(databaseName: String): Unit = {
    val result = execInContainer("createdb", "-U", getUsername, databaseName)
    if (result.getExitCode != 0) {
      throw new RuntimeException(
        s"Executing `createdb` in container failed: exit code ${result.getExitCode}, " +
          s"stdout:\n${result.getStdout}\nstderr:\n${result.getStderr}"
      )
    }
  }

  private def runMigrations(databaseName: String): Unit = {
    val configMap = Map(
      "profile" -> "slick.jdbc.PostgresProfile$",
      "db.url" -> s"jdbc:postgresql://$getHost:$getFirstMappedPort/$databaseName?reWriteBatchedInserts=true",
      "db.user" -> getUsername,
      "db.password" -> getPassword
    )
    val migrationSettings = ConfigFactory.fromEnvironment(configMap)
    val db = DatabaseConfig.forConfig[JdbcProfile](path = "", migrationSettings).db

    try {
      // `group(true)` to group all pending migrations to a single DB transaction
      val flyway = Flyway
        .configure()
        // migrations aren't stored in the default location (`classpath:db/migration`)
        .locations(s"filesystem:${System.getProperty("user.dir")}/tools/sql")
        .dataSource(new DatabaseDatasource(db))
        // group all migrations into a single database transaction
        .group(true)
        .load()
      flyway.migrate()
    } finally
      db.close()
  }
}

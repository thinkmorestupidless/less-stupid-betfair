package com.thinkmorestupidless.utils

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import com.thinkmorestupidless.utils.ConfigFactory.Environment
import com.typesafe.config.Config
import org.scalatest.{BeforeAndAfterAll, TestSuite}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

trait IntegrationSpec extends TestSuite with BeforeAndAfterAll {

  private lazy val postgresProperties: Environment = Map(
    "POSTGRES_HOST" -> Postgres.instance.getHost,
    "POSTGRES_PORT" -> Postgres.instance.getFirstMappedPort.toString,
    "POSTGRES_USER" -> Postgres.instance.getUsername,
    "POSTGRES_PASSWORD" -> Postgres.instance.getPassword,
    "POSTGRES_DB" -> Postgres.instance.initNewDatabase()
  )

  protected lazy val config: Config = ConfigFactory.forIntegrationTesting(postgresProperties)
  protected implicit lazy val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig(path = "slick", config)

  val testKit: ActorTestKit = ActorTestKit(config)
  implicit val typedSystem: ActorSystem[Nothing] = testKit.system
  //  implicit val materializer: Materializer = Materializer(typedSystem.classicSystem)
  implicit val ec: ExecutionContext = typedSystem.executionContext

  override protected def afterAll(): Unit = {
    dbConfig.db.close()
    super.afterAll()
  }
}

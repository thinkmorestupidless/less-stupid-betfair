package com.thinkmorestupidless.utils

import com.typesafe.config.{Config, ConfigFactory => TypesafeConfigFactory}

import scala.collection.JavaConverters._

object ConfigFactory {

  type Environment = Map[String, String]

  // we don't want to read the entire 'application.conf' here, so we use empty config
  def forUnitTesting: Config = TypesafeConfigFactory.empty()

  def forIntegrationTesting(environmentOverrides: Environment): Config =
    TypesafeConfigFactory
      .empty()
      .withFallback(fromConfigFile("integration-test"))
      .withFallback(fromEnvironment(environmentOverrides))
      .resolve()

  def fromConfigFile(file: String): Config =
    TypesafeConfigFactory.parseResourcesAnySyntax(file)

  def fromEnvironment(environment: Environment): Config =
    TypesafeConfigFactory.parseMap(environment.asJava)
}

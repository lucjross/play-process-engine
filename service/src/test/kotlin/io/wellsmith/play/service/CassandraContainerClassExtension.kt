package io.wellsmith.play.service

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.PropertySource
import org.testcontainers.containers.CassandraContainer

/**
 * JUnit 4 adapter
 */
class CassandraContainerClassExtension(imageName: String):
    BeforeAllCallback, AfterAllCallback {

  internal val container = KCassandraContainer(imageName)

  override fun beforeAll(context: ExtensionContext?) {
    container.start()
  }

  override fun afterAll(context: ExtensionContext?) {
    container.stop()
  }
}

/**
 * Java type adapter
 */
internal class KCassandraContainer(imageName: String): CassandraContainer<KCassandraContainer>(imageName)

/**
 * Provides the randomly assigned container port to the application context
 */
// https://www.testcontainers.org/usage/generic_containers.html#accessing-a-container-from-tests
abstract class AbstractMappedPortPropertyInitializer :
    ApplicationContextInitializer<ConfigurableApplicationContext> {

  internal abstract fun container(): KCassandraContainer
  internal abstract fun exposedPort(): Int

  override fun initialize(applicationContext: ConfigurableApplicationContext) {
    applicationContext.environment.propertySources.addFirst(
        object : PropertySource<String>("mappedPortTestPropertySource") {
          override fun getProperty(name: String) =
              when (name) {
                "spring.data.cassandra.port" ->
                  container().getMappedPort(exposedPort())
                else -> null
              }
        })
  }
}
package io.wellsmith.playprocessengine.restapp

import io.wellsmith.playprocessengine.persistence.cassandra.PlayCassandraRepositoryConfiguration
import io.wellsmith.playprocessengine.service.PlayServiceConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(
    scanBasePackageClasses = [
      Application::class,
      PlayServiceConfiguration::class,
      PlayCassandraRepositoryConfiguration::class])
class Application {
}

fun main(args: Array<String>) {
  SpringApplication.run(Application::class.java, *args)
}
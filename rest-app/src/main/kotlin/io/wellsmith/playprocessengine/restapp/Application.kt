package io.wellsmith.playprocessengine.restapp

import io.wellsmith.playprocessengine.serde.BPMN20Serde
import io.wellsmith.playprocessengine.service.PlayServiceConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.oxm.jaxb.Jaxb2Marshaller

@SpringBootApplication(
    scanBasePackageClasses = [Application::class, PlayServiceConfiguration::class])
class Application {

  @Bean
  fun bpmn20Marshaller() = BPMN20Serde.marshaller()

  @Bean
  fun bpmn20Serde(bpmn20Marshaller: Jaxb2Marshaller) = BPMN20Serde(bpmn20Marshaller)
}

fun main(args: Array<String>) {
  SpringApplication.run(Application::class.java, *args)
}
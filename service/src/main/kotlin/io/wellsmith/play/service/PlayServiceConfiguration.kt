package io.wellsmith.play.service

import io.wellsmith.play.serde.BPMN20Serde
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.oxm.jaxb.Jaxb2Marshaller

@Configuration
@ComponentScan
class PlayServiceConfiguration {

  @Bean
  fun bpmn20Marshaller() = BPMN20Serde.defaultMarshaller

  @Bean
  fun bpmn20Serde(bpmn20Marshaller: Jaxb2Marshaller) = BPMN20Serde(bpmn20Marshaller)
}
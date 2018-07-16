package io.wellsmith.playprocessengine.service

import io.wellsmith.playprocessengine.domain.bpmn.BPMN20XMLEntity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PlayServiceConfiguration {

  @Bean
  fun bpmn20BundleCache(): MutableCollection<BPMN20XMLEntity> = mutableListOf()
}
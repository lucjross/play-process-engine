package io.wellsmith.play.service

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.defaultNanoClock
import io.wellsmith.play.engine.visitor.Visitors
import io.wellsmith.play.persistence.api.ElementVisitRepository
import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.persistence.api.ProcessInstanceRepository
import io.wellsmith.play.serde.BPMN20Serde
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import java.time.Clock
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

@Configuration
@ComponentScan
class PlayServiceConfiguration {

  @Bean
  fun bpmn20Marshaller() = BPMN20Serde.defaultMarshaller()

  @Bean
  fun bpmn20Serde(bpmn20Marshaller: Jaxb2Marshaller) = BPMN20Serde(bpmn20Marshaller)

  @Bean
  fun executorService(): ForkJoinPool = ForkJoinPool.commonPool()

  @Bean
  fun clock(): Clock = defaultNanoClock

  @Bean
  fun playEngineConfiguration(processInstanceRepository: ProcessInstanceRepository<*>,
                              elementVisitRepository: ElementVisitRepository<*>,
                              entityFactory: EntityFactory,
                              executorService: ExecutorService,
                              clock: Clock) =
      PlayEngineConfiguration(
          processInstanceRepository,
          elementVisitRepository,
          entityFactory,
          executorService,
          clock)

  @Bean
  fun visitors(playEngineConfiguration: PlayEngineConfiguration) =
      Visitors(playEngineConfiguration)
}
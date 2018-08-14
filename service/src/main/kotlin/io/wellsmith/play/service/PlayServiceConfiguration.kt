package io.wellsmith.play.service

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.defaultNanoClock
import io.wellsmith.play.engine.testhook.Synchronizer
import io.wellsmith.play.engine.visitor.VisitorsWrapper
import io.wellsmith.play.persistence.api.ActivityStateChangeRepository
import io.wellsmith.play.persistence.api.BPMN20XMLRepository
import io.wellsmith.play.persistence.api.ElementVisitRepository
import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.persistence.api.ProcessInstanceRepository
import io.wellsmith.play.serde.BPMN20Serde
import org.springframework.beans.factory.annotation.Autowired
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
  fun playEngineConfiguration(bpmn20Serde: BPMN20Serde,
                              bpmn20XMLRepository: BPMN20XMLRepository<*>,
                              processInstanceRepository: ProcessInstanceRepository<*>,
                              elementVisitRepository: ElementVisitRepository<*>,
                              activityStateChangeRepository: ActivityStateChangeRepository<*>,
                              entityFactory: EntityFactory,
                              executorService: ExecutorService,
                              clock: Clock,
                              @Autowired(required = false) synchronizer: Synchronizer?) =
      PlayEngineConfiguration(
          bpmn20Serde,
          bpmn20XMLRepository,
          processInstanceRepository,
          elementVisitRepository,
          activityStateChangeRepository,
          entityFactory,
          executorService,
          clock,
          synchronizer)

  @Bean
  fun processCache(playEngineConfiguration: PlayEngineConfiguration) =
      playEngineConfiguration.processCache

  @Bean
  fun activeProcessInstanceCache(playEngineConfiguration: PlayEngineConfiguration) =
      playEngineConfiguration.activeProcessInstanceCache

  @Bean
  fun activityStateChangeHandler(playEngineConfiguration: PlayEngineConfiguration) =
      playEngineConfiguration.activityStateChangeHandler

  @Bean
  fun visitorsWrapper(playEngineConfiguration: PlayEngineConfiguration) =
      VisitorsWrapper(playEngineConfiguration)
}
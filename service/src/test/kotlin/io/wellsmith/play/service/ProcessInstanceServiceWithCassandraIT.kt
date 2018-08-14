package io.wellsmith.play.service

import io.wellsmith.play.domain.BPMN20XMLEntity
import io.wellsmith.play.persistence.api.BPMN20XMLRepository
import io.wellsmith.play.persistence.cassandra.PlayCassandraRepositoryConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.Optional
import java.util.UUID

@SpringJUnitConfig(
    classes = [
      PlayServiceConfiguration::class,
      PlayCassandraRepositoryConfiguration::class],
    initializers = [
      ConfigFileApplicationContextInitializer::class,
      ProcessInstanceServiceWithCassandraIT.MappedPortPropertyInitializer::class])
class ProcessInstanceServiceWithCassandraIT {

  companion object {

    @RegisterExtension
    @JvmField
    val cdbContainer = CassandraContainerClassExtension("cassandra:3")
        .apply {
          container.addExposedPort(9042)
        }
  }

  @MockBean
  private lateinit var bpmn20XMLRepository: BPMN20XMLRepository<*>

  @MockBean
  private lateinit var manualTaskService: ManualTaskService

  @Autowired
  private lateinit var processInstanceService: ProcessInstanceService

  @Test
  fun `instantiateProcess should succeed`() {

    val filename = "flownodes-without-incoming-sequenceflows.bpmn20.xml"
    val xml = Thread.currentThread().contextClassLoader.getResourceAsStream(filename)
        .readBytes().toString(Charsets.UTF_8)
    val bpmn20XMLEntityId = UUID.randomUUID()
    Mockito.`when`(bpmn20XMLRepository.findById(bpmn20XMLEntityId))
        .thenReturn(Optional.of(object: BPMN20XMLEntity {
          override val originalFilename = ""
          override val document = xml
          override val bundleId = UUID.randomUUID()
          override val id = bpmn20XMLEntityId
        }))

    val processInstanceId = processInstanceService.instantiateProcess(
        bpmn20XMLEntityId, "test-process-1")
  }



  class MappedPortPropertyInitializer: AbstractMappedPortPropertyInitializer() {
    override fun container() = cdbContainer.container
    override fun exposedPort() = 9042
  }
}
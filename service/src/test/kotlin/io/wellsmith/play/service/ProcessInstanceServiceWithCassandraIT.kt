package io.wellsmith.play.service

import io.wellsmith.play.persistence.cassandra.PlayCassandraRepositoryConfiguration
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.UUID

@SpringJUnitConfig(
    classes = [
      PlayServiceConfiguration::class,
      PlayCassandraRepositoryConfiguration::class],
    initializers = [ConfigFileApplicationContextInitializer::class])
class ProcessInstanceServiceWithCassandraIT {

  @MockBean
  private lateinit var bpmn20XMLService: BPMN20XMLService<*>

  @Autowired
  private lateinit var processInstanceService: BPMN20ProcessInstanceService<*>

  @Test
  fun `test`() {

    val filename = "flownodes-without-incoming-sequenceflows.bpmn20.xml"
    val xml = Thread.currentThread().contextClassLoader.getResourceAsStream(filename)
        .readBytes().toString(Charsets.UTF_8)
    val bpmn20XMLEntityId = UUID.randomUUID()
    Mockito.`when`(bpmn20XMLService.getDefinitionsXML(bpmn20XMLEntityId))
        .thenReturn(xml)

    val processInstanceId = processInstanceService.instantiateProcess(
        bpmn20XMLEntityId, "test-process-1")
  }
}
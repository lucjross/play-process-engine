package io.wellsmith.playprocessengine.service

import io.wellsmith.playprocessengine.persistence.cassandra.PlayCassandraRepositoryConfiguration
import io.wellsmith.playprocessengine.service.command.BPMN20XML
import org.cassandraunit.spring.CassandraUnitDependencyInjectionIntegrationTestExecutionListener
import org.cassandraunit.spring.EmbeddedCassandra
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig(
    classes = [
      PlayServiceConfiguration::class,
      PlayCassandraRepositoryConfiguration::class],
    initializers = [ConfigFileApplicationContextInitializer::class])
@TestExecutionListeners(
    listeners = [FixedCassandraUnitTestExecutionListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@EmbeddedCassandra
class BPMN20XMLServiceWithCassandraIT {

  @Autowired
  private lateinit var bpmn20XMLService: BPMN20XMLService<*>

  //todo
  @Test
  fun `createBundle should insert new bundle`() {

    val filename = "minimal.bpmn20.xml"
    val xml = Thread.currentThread().contextClassLoader.getResourceAsStream(filename)
        .readBytes().toString(Charsets.UTF_8)
    val bundleId = bpmn20XMLService.createBundle(listOf(
        BPMN20XML(filename, xml, null)
    ))

    val definitionsIds = bpmn20XMLService.getDefinitionsIdsInBundle(bundleId)
    val retrievedXml = bpmn20XMLService.getDefinitionsXML(definitionsIds.first())
    Assertions.assertEquals(xml, retrievedXml)
  }
}
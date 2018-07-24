package io.wellsmith.play.service

import io.wellsmith.play.persistence.cassandra.PlayCassandraRepositoryConfiguration
import io.wellsmith.play.service.command.BPMN20XML
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig(
    classes = [
      PlayServiceConfiguration::class,
      PlayCassandraRepositoryConfiguration::class],
    initializers = [
      ConfigFileApplicationContextInitializer::class,
      BPMN20XMLServiceWithCassandraIT.MappedPortPropertyInitializer::class])
class BPMN20XMLServiceWithCassandraIT {

  companion object {

    @RegisterExtension
    @JvmField
    val cdbContainer = CassandraContainerClassExtension("cassandra:3")
        .apply {
          container.addExposedPort(9042)
        }
  }

  @Autowired
  private lateinit var bpmn20XMLService: BPMN20XMLService<*>

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



  class MappedPortPropertyInitializer: AbstractMappedPortPropertyInitializer() {
    override fun container() = cdbContainer.container
    override fun exposedPort() = 9042
  }
}
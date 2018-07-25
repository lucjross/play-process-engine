package io.wellsmith.play.restapp.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.wellsmith.play.persistence.api.BPMN20XMLRepository
import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.persistence.cassandra.BPMN20XMLCassandraRepository
import io.wellsmith.play.persistence.cassandra.CassandraEntityFactory
import io.wellsmith.play.persistence.cassandra.PlayCassandraRepositoryConfiguration
import io.wellsmith.play.persistence.cassandra.entity.BPMN20XMLCassandraEntity
import io.wellsmith.play.restapp.Application
import io.wellsmith.play.serde.BPMN20Serde
import io.wellsmith.play.service.BPMN20XMLService
import io.wellsmith.play.service.PlayServiceConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.Collections
import java.util.Optional
import java.util.UUID

/**
 * Tests from web layer to service layer -- repositories mocked
 */
@SpringJUnitWebConfig
@TestPropertySource(properties = [
  "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration"
])
@AutoConfigureMockMvc
class BPMN20BundleControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var bpmn20XMLRepository: BPMN20XMLRepository<*>

  @Test
  fun `can post and retrieve a zipped XML bundle`() {

    val filename = "two bpmn xml files.zip"
    val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(filename)
    val multipartFile = MockMultipartFile("file", filename, "application/zip", inputStream)
    val postResult = mockMvc.perform(MockMvcRequestBuilders.multipart("/bpmn20/bundle")
        .file(multipartFile))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
        .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION))
        .andReturn()

    val bundleLocation = postResult.response.getHeader(HttpHeaders.LOCATION)
    val bundleId = bundleLocation!!.substringAfterLast("/")
    val mockXml = "<?xml..."
    val mockId = UUID.randomUUID()
    Mockito.`when`(bpmn20XMLRepository.findByBundleId(UUID.fromString(bundleId)))
        .thenReturn(Collections.nCopies(2,
            BPMN20XMLCassandraEntity(mockId, "", mockXml, UUID.fromString(bundleId))
        ))
    val getBundleResult = mockMvc.perform(MockMvcRequestBuilders.get("/bpmn20/bundle/$bundleId"))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
        .andReturn()
    val ids = objectMapper.readValue(
        getBundleResult.response.contentAsString, Array<String>::class.java)
    Assertions.assertEquals(2, ids.size)

    Mockito.`when`(bpmn20XMLRepository.findById(mockId))
        .thenReturn(Optional.of(BPMN20XMLCassandraEntity(mockId, "", mockXml, UUID.fromString(bundleId))))
    val getXMLResult = mockMvc.perform(MockMvcRequestBuilders.get("/bpmn20/definitions/${ids[0]}"))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
        .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
        .andExpect(MockMvcResultMatchers.content().string(mockXml))
  }

  @TestConfiguration
  @SpringBootApplication(
      scanBasePackageClasses = [Application::class],
      exclude = [CassandraDataAutoConfiguration::class])
  class TestContextConfiguration {

    @Bean
    fun entityFactory(): EntityFactory = CassandraEntityFactory()

    @Bean
    fun bpmn20XMLRepository(): BPMN20XMLRepository<*> =
        Mockito.mock(BPMN20XMLCassandraRepository::class.java)

    @Bean
    fun bpmn20XMLService(bpmn20XMLRepository: BPMN20XMLRepository<*>,
                         entityFactory: EntityFactory): BPMN20XMLService<*> =
        BPMN20XMLService(bpmn20XMLRepository, entityFactory, BPMN20Serde())
  }

}
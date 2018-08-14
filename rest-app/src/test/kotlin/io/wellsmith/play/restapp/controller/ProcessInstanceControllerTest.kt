package io.wellsmith.play.restapp.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.wellsmith.play.domain.ElementVisitEntity
import io.wellsmith.play.domain.elementKeyOf
import io.wellsmith.play.engine.activity.ActivityLifecycle
import io.wellsmith.play.engine.now
import io.wellsmith.play.engine.testhook.Synchronizer
import io.wellsmith.play.persistence.api.ActivityStateChangeRepository
import io.wellsmith.play.persistence.api.BPMN20XMLRepository
import io.wellsmith.play.persistence.api.ElementVisitRepository
import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.persistence.api.ProcessInstanceRepository
import io.wellsmith.play.persistence.cassandra.CassandraEntityFactory
import io.wellsmith.play.persistence.cassandra.entity.BPMN20XMLCassandraEntity
import io.wellsmith.play.persistence.cassandra.entity.ElementVisitCassandraEntity
import io.wellsmith.play.persistence.cassandra.entity.ProcessInstanceCassandraEntity
import io.wellsmith.play.restapp.Application
import io.wellsmith.play.service.PlayServiceConfiguration
import io.wellsmith.play.service.response.InstantiatedProcess
import io.wellsmith.play.service.response.ProcessIdToBPMN20XMLEntityId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Tests from web layer to service layer -- repositories mocked
 */
@SpringJUnitWebConfig
@TestPropertySource(properties = [
  "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration"
])
@AutoConfigureMockMvc
class ProcessInstanceControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var synchronizer: Synchronizer

  @MockBean private lateinit var activityStateChangeRepository: ActivityStateChangeRepository<*>
  @MockBean private lateinit var bpmn20XMLRepository: BPMN20XMLRepository<*>
  @MockBean private lateinit var processInstanceRepository: ProcessInstanceRepository<*>
  @MockBean private lateinit var elementVisitRepository: ElementVisitRepository<*>

  @Test
  fun `isCompleted should work from instantiation to completion`() {

    // create bundle
    val filename = "single-task.bpmn20.xml"
    val xmlBytes = Thread.currentThread().contextClassLoader.getResourceAsStream(filename)
        .readBytes()
    val postResult = postBundle(filename, xmlBytes)

    // get process ids
    val bundleLocation = postResult.response.getHeader(HttpHeaders.LOCATION)
    val bundleId = bundleLocation!!.substringAfterLast("/")
    val mockXmlEntityId = UUID.randomUUID()
    val ids = getProcessIds(bundleId, mockXmlEntityId, xmlBytes)

    val instantiatedProcess = postInstantiateProcess(mockXmlEntityId, xmlBytes, bundleId, ids)

    // get isCompleted
    val elementVisitEntities = mutableListOf<ElementVisitEntity>()
    elementVisitEntities.addAndStub("hi", instantiatedProcess, null)
    elementVisitEntities.addAndStub("hi", "manual-task-1", instantiatedProcess, "hi")
    elementVisitEntities.addAndStub("manual-task-1", instantiatedProcess, elementKeyOf("hi", "manual-task-1"))

    synchronizer.lock("processInstance.isCompleted()")
    val completed1 = getCompleted(instantiatedProcess)
    Assertions.assertFalse(completed1)

    // complete Manual Task
    mockMvc.perform(
        MockMvcRequestBuilders.post("/bpmn20/manualTask/${instantiatedProcess.processInstanceEntityId}/manual-task-1/completeWork"))
        .andExpect(MockMvcResultMatchers.status().isOk)

    elementVisitEntities.addAndStub("manual-task-1", "bye", instantiatedProcess, "manual-task-1")
    elementVisitEntities.addAndStub("bye", instantiatedProcess, elementKeyOf("manual-task-1", "bye"))

    synchronizer.lock("processInstance.isCompleted()")
    val completed2 = getCompleted(instantiatedProcess)
    Assertions.assertTrue(completed2)
  }

  private fun postBundle(filename: String, xmlBytes: ByteArray): MvcResult {
    val multipartFile = MockMultipartFile("file", filename, MediaType.APPLICATION_XML_VALUE, xmlBytes)
    val postResult = mockMvc.perform(MockMvcRequestBuilders.multipart("/bpmn20/bundle")
        .file(multipartFile))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
        .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION))
        .andReturn()
    return postResult
  }

  private fun getProcessIds(bundleId: String, mockXmlEntityId: UUID, xmlBytes: ByteArray): Array<ProcessIdToBPMN20XMLEntityId> {
    whenever(bpmn20XMLRepository.findByBundleId(UUID.fromString(bundleId))) doReturn
        listOf(BPMN20XMLCassandraEntity(mockXmlEntityId, "",
            xmlBytes.toString(Charsets.UTF_8), UUID.fromString(bundleId)))
    val getBundleResult = mockMvc.perform(MockMvcRequestBuilders.get("/bpmn20/bundle/$bundleId"))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
        .andReturn()
    val ids = objectMapper.readValue(
        getBundleResult.response.contentAsString, Array<ProcessIdToBPMN20XMLEntityId>::class.java)
    Assertions.assertEquals(1, ids.size)
    return ids
  }

  private fun postInstantiateProcess(mockXmlEntityId: UUID, xmlBytes: ByteArray, bundleId: String, ids: Array<ProcessIdToBPMN20XMLEntityId>): InstantiatedProcess {
    whenever(bpmn20XMLRepository.findById(mockXmlEntityId)) doReturn
        Optional.of(BPMN20XMLCassandraEntity(mockXmlEntityId, "",
            xmlBytes.toString(Charsets.UTF_8), UUID.fromString(bundleId)))
    val instantiateResult = mockMvc.perform(
        MockMvcRequestBuilders.post("/bpmn20/processInstance")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ids[0])))
        .andExpect(MockMvcResultMatchers.status().isCreated)
        .andReturn()
    val instantiatedProcess = objectMapper.readValue(
        instantiateResult.response.contentAsString, InstantiatedProcess::class.java)
    Assertions.assertEquals(ids[0].bpmn20XMLEntityId, instantiatedProcess.bpmn20XMLEntityId)
    Assertions.assertEquals(ids[0].processId, instantiatedProcess.processId)
    return instantiatedProcess
  }

  private fun getCompleted(instantiatedProcess: InstantiatedProcess): Boolean {
    whenever(processInstanceRepository.findById(instantiatedProcess.processInstanceEntityId)) doReturn
        Optional.of(ProcessInstanceCassandraEntity(
            instantiatedProcess.processInstanceEntityId,
            instantiatedProcess.bpmn20XMLEntityId,
            instantiatedProcess.processId))
    val completedResult1 = mockMvc.perform(
        MockMvcRequestBuilders.get("/bpmn20/processInstance/${instantiatedProcess.processInstanceEntityId}/completed"))
        .andExpect(MockMvcResultMatchers.status().isOk)
        .andReturn()
    return objectMapper.readValue(
        completedResult1.response.contentAsString, Boolean::class.java)
  }

  private fun MutableList<ElementVisitEntity>.addAndStub(baseElementId: String,
                                                         instantiatedProcess: InstantiatedProcess,
                                                         fromFlowElementKey: String?) {
    add(ElementVisitCassandraEntity(UUID.randomUUID(), instantiatedProcess.bpmn20XMLEntityId,
        instantiatedProcess.processId, instantiatedProcess.processInstanceEntityId, baseElementId,
        null, null, ElementVisitCassandraEntity.ElementType.FLOW_NODE,
        fromFlowElementKey, null,
        now().minus(1, ChronoUnit.DAYS)))
    whenever(elementVisitRepository.findByProcessInstanceEntityId(instantiatedProcess.processInstanceEntityId)) doReturn
        this
  }

  private fun MutableList<ElementVisitEntity>.addAndStub(sourceRefId: String, targetRefId: String,
                                                         instantiatedProcess: InstantiatedProcess,
                                                         fromFlowElementKey: String?) {
    add(ElementVisitCassandraEntity(UUID.randomUUID(), instantiatedProcess.bpmn20XMLEntityId,
        instantiatedProcess.processId, instantiatedProcess.processInstanceEntityId, null,
        sourceRefId, targetRefId, ElementVisitCassandraEntity.ElementType.SEQUENCE_FLOW,
        fromFlowElementKey, null,
        now().minus(1, ChronoUnit.DAYS)))
    whenever(elementVisitRepository.findByProcessInstanceEntityId(instantiatedProcess.processInstanceEntityId)) doReturn
        this
  }



  @TestConfiguration
  @SpringBootApplication(
      scanBasePackageClasses = [
        Application::class,
        PlayServiceConfiguration::class],
      exclude = [CassandraDataAutoConfiguration::class])
  class TestContextConfiguration {

    @Bean
    fun entityFactory(): EntityFactory = CassandraEntityFactory()

    @Bean
    fun testSynchronizer(): Synchronizer = object: TestSynchronizer() {

      override fun update(action: String, value: Any?) {
        when (action) {
          "state change: manual-task-1 to ${ActivityLifecycle.State.ACTIVE}" -> {
            unlock("processInstance.isCompleted()")
          }
          "visit: bye" -> {
            unlock("processInstance.isCompleted()")
          }
        }
      }
    }
  }

}

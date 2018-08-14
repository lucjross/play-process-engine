package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.FlowElementGraph
import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.activity.ActivityLifecycle
import io.wellsmith.play.persistence.api.ActivityStateChangeRepository
import io.wellsmith.play.persistence.api.BPMN20XMLRepository
import io.wellsmith.play.persistence.api.ElementVisitRepository
import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.persistence.api.ProcessInstanceRepository
import io.wellsmith.play.serde.BPMN20Serde
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.omg.spec.bpmn._20100524.model.ObjectFactory
import org.omg.spec.bpmn._20100524.model.TProcess
import org.omg.spec.bpmn._20100524.model.TTask
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ExecutorService

@ExtendWith(MockitoExtension::class)
class ActivityVisitorTest {

  private val objectFactory = ObjectFactory()
  @Mock private lateinit var stateChangeInterceptor: ActivityLifecycle.StateChangeInterceptor
  @Mock private lateinit var bpmn20Serde: BPMN20Serde
  @Mock private lateinit var bpmn20XMLRepository: BPMN20XMLRepository<*>
  @Mock private lateinit var processInstanceRepository: ProcessInstanceRepository<*>
  @Mock private lateinit var elementVisitRepository: ElementVisitRepository<*>
  @Mock private lateinit var activityStateChangeRepository: ActivityStateChangeRepository<*>
  @Mock private lateinit var entityFactory: EntityFactory
  @Mock private lateinit var executorService: ExecutorService
  @Mock private lateinit var clock: Clock
  @InjectMocks private lateinit var playEngineConfiguration: PlayEngineConfiguration

  @Test
  fun `newActivityLifecycle should invoke the right constructor`() {

    val task = TTask().apply { id = "id" }
    val process = TProcess()
        .apply { flowElement.add(
            objectFactory.createTask(task)) }
    val entityId = UUID.randomUUID()
    val processInstance = ProcessInstance(
        FlowElementGraph(process), "id", UUID.randomUUID(), entityId,
        stateChangeInterceptor)
    val activityVisitor = TaskVisitor(processInstance, playEngineConfiguration,
        Visitors(playEngineConfiguration), process.flowElement[0].value as TTask)

    val activityLifecycle = activityVisitor.newActivityLifecycle(
        task, processInstance, null)
    Assertions.assertTrue(activityLifecycle is net.sf.cglib.proxy.Factory)
    Assertions.assertEquals(task.id, activityLifecycle.activity.id)
  }
}
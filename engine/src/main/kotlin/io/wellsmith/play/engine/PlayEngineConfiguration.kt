package io.wellsmith.play.engine

import io.wellsmith.play.domain.ActivityStateChangeEntity
import io.wellsmith.play.domain.ElementVisitEntity
import io.wellsmith.play.domain.Entity
import io.wellsmith.play.domain.ProcessInstanceEntity
import io.wellsmith.play.engine.activity.ActivityStateChangeHandler
import io.wellsmith.play.engine.activity.InMemoryActivityStateChangeHandlingQueue
import io.wellsmith.play.engine.activity.ActivityStateChangeHandlingQueueListener
import io.wellsmith.play.engine.testhook.Synchronizer
import io.wellsmith.play.engine.visitor.InMemoryVisitationQueue
import io.wellsmith.play.engine.visitor.InMemoryVisitationQueueListener
import io.wellsmith.play.persistence.api.ActivityStateChangeRepository
import io.wellsmith.play.persistence.api.BPMN20XMLRepository
import io.wellsmith.play.persistence.api.ElementVisitRepository
import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.persistence.api.ProcessInstanceRepository
import io.wellsmith.play.serde.BPMN20Serde
import org.springframework.data.repository.CrudRepository
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ExecutorService
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

open class PlayEngineConfiguration(
    private val bpmn20Serde: BPMN20Serde,
    private val bpmn20XMLRepository: BPMN20XMLRepository<*>,
    private val processInstanceRepository: ProcessInstanceRepository<*>,
    private val elementVisitRepository: ElementVisitRepository<*>,
    private val activityStateChangeRepository: ActivityStateChangeRepository<*>,
    val entityFactory: EntityFactory,
    val executorService: ExecutorService,
    val clock: Clock,
    synchronizer: Synchronizer?) {

  private val _visitationQueue = InMemoryVisitationQueue()
  private val _activityStateChangeQueue = InMemoryActivityStateChangeHandlingQueue()
  private val _synchronizer: Synchronizer = synchronizer
      ?: object: Synchronizer {}

  val processCache = ProcessCache(bpmn20XMLRepository, bpmn20Serde)
  val activeProcessInstanceCache = ActiveProcessInstanceCache(
      processInstanceRepository, elementVisitRepository,
      activityStateChangeRepository, processCache,
      entityFactory, clock,
      activityStateChangeQueue())
  val activityStateChangeHandler = ActivityStateChangeHandler(this)

  init {
    InMemoryVisitationQueueListener(_visitationQueue, _synchronizer).listen()
    ActivityStateChangeHandlingQueueListener(
        _activityStateChangeQueue,
        activityStateChangeHandler,
        _synchronizer).listen()
  }

  @Suppress("UNCHECKED_CAST")
  internal fun <T: Entity> repositoryOf(cls: KClass<T>) =
      when {
        cls.isSubclassOf(ElementVisitEntity::class) -> elementVisitRepository
        cls.isSubclassOf(ProcessInstanceEntity::class) -> processInstanceRepository
        cls.isSubclassOf(ActivityStateChangeEntity::class) -> activityStateChangeRepository
        else -> TODO()
      } as CrudRepository<T, UUID>

  internal fun visitationQueue() = _visitationQueue
  fun activityStateChangeQueue() = _activityStateChangeQueue
  fun synchronizer() = _synchronizer
}
package io.wellsmith.play.engine

import io.wellsmith.play.domain.ElementVisitEntity
import io.wellsmith.play.domain.Entity
import io.wellsmith.play.domain.ProcessInstanceEntity
import io.wellsmith.play.persistence.api.ElementVisitRepository
import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.persistence.api.ProcessInstanceRepository
import org.springframework.data.repository.CrudRepository
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ExecutorService
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class PlayEngineConfiguration(private val processInstanceRepository: ProcessInstanceRepository<*>,
                              private val elementVisitRepository: ElementVisitRepository<*>,
                              val entityFactory: EntityFactory,
                              val executorService: ExecutorService,
                              val clock: Clock) {

  @Suppress("UNCHECKED_CAST")
  fun <T: Entity> repositoryOf(cls: KClass<T>) =
      when {
        cls.isSubclassOf(ElementVisitEntity::class) -> elementVisitRepository
        cls.isSubclassOf(ProcessInstanceEntity::class) -> processInstanceRepository
        else -> TODO()
      } as CrudRepository<T, UUID>
}
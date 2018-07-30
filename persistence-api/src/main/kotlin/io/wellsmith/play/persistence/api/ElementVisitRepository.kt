package io.wellsmith.play.persistence.api

import io.wellsmith.play.domain.ElementVisitEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ElementVisitRepository<T: ElementVisitEntity>: CrudRepository<T, UUID> {

  fun findByProcessInstanceEntityId(processInstanceEntityId: UUID): List<T>
}
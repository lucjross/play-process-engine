package io.wellsmith.play.persistence.api

import io.wellsmith.play.domain.ActivityStateChangeEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ActivityStateChangeRepository<T: ActivityStateChangeEntity>: CrudRepository<T, UUID> {

  fun findByProcessInstanceEntityId(processInstanceEntityId: UUID): List<T>
}
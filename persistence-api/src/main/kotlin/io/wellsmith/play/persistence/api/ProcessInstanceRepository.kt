package io.wellsmith.play.persistence.api

import io.wellsmith.play.domain.ProcessInstanceEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ProcessInstanceRepository<T: ProcessInstanceEntity>: CrudRepository<T, UUID> {
}
package io.wellsmith.playprocessengine.persistence.api

import io.wellsmith.playprocessengine.domain.BPMN20XMLEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface BPMN20XMLRepository<T: BPMN20XMLEntity>: CrudRepository<T, UUID> {

  fun findByBundleId(bundleId: UUID): List<T>
}
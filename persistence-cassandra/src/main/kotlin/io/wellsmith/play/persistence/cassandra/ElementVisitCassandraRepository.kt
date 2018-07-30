package io.wellsmith.play.persistence.cassandra

import io.wellsmith.play.persistence.api.ElementVisitRepository
import io.wellsmith.play.persistence.cassandra.entity.ElementVisitCassandraEntity
import org.springframework.data.cassandra.repository.CassandraRepository
import java.util.UUID

interface ElementVisitCassandraRepository:
    ElementVisitRepository<ElementVisitCassandraEntity>,
    CassandraRepository<ElementVisitCassandraEntity, UUID> {

  override fun findByProcessInstanceEntityId(processInstanceEntityId: UUID):
      List<ElementVisitCassandraEntity>

}
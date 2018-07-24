package io.wellsmith.play.persistence.cassandra.entity

import io.wellsmith.play.domain.ProcessInstanceEntity
import java.util.UUID

class ProcessInstanceCassandraEntity(override val id: UUID): ProcessInstanceEntity {
}
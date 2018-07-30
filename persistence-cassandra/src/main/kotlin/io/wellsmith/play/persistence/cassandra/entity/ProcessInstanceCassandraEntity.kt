package io.wellsmith.play.persistence.cassandra.entity

import io.wellsmith.play.domain.ProcessInstanceEntity
import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.mapping.Table
import java.util.UUID

@Table("ProcessInstance")
data class ProcessInstanceCassandraEntity(
    @Id override val id: UUID,
    override val bpmn20XMLEntityId: UUID,
    override val processId: String): ProcessInstanceEntity {
}
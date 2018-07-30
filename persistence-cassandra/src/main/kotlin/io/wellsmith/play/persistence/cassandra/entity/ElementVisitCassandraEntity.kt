package io.wellsmith.play.persistence.cassandra.entity

import io.wellsmith.play.domain.ElementVisitEntity
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("ElementVisit")
class ElementVisitCassandraEntity(
    override val id: UUID,
    override val bpmn20XMLEntityId: UUID,
    override val processId: String,
    @PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    override val processInstanceEntityId: UUID,
    override val baseElementId: String?,
    override val fromFlowNodeId: String?,
    @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    override val time: Instant): ElementVisitEntity {
}
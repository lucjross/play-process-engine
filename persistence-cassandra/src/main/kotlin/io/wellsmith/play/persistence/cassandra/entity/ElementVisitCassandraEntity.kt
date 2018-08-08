package io.wellsmith.play.persistence.cassandra.entity

import io.wellsmith.play.domain.FlowNodeVisitEntity
import io.wellsmith.play.domain.SequenceFlowVisitEntity
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
    override val sourceRefId: String?,
    override val targetRefId: String?,
    val elementType: ElementType,
    override val fromFlowElementId: String?,
    override val splitCorrelationId: UUID?,
    @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    override val time: Instant
): FlowNodeVisitEntity, SequenceFlowVisitEntity {

  init {
    if (elementType != ElementType.SEQUENCE_FLOW && splitCorrelationId != null) {
      throw IllegalArgumentException("splitCorrelationId not applicable to elementType $elementType")
    }
  }

    override fun elementKey() = when (elementType) {
      ElementType.FLOW_NODE -> super<FlowNodeVisitEntity>.elementKey()
      ElementType.SEQUENCE_FLOW -> super<SequenceFlowVisitEntity>.elementKey()
    }

    enum class ElementType {
      FLOW_NODE, SEQUENCE_FLOW
    }
}
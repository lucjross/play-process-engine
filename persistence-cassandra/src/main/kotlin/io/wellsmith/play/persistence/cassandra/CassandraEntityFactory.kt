package io.wellsmith.play.persistence.cassandra

import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.persistence.cassandra.entity.BPMN20XMLCassandraEntity
import io.wellsmith.play.persistence.cassandra.entity.ElementVisitCassandraEntity
import io.wellsmith.play.persistence.cassandra.entity.ProcessInstanceCassandraEntity
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class CassandraEntityFactory: EntityFactory {

  override fun bpmn20XMLEntity(id: UUID,
                               originalFilename: String,
                               document: String,
                               bundleId: UUID) =
      BPMN20XMLCassandraEntity(id, originalFilename, document, bundleId)

  override fun processInstanceEntity(id: UUID,
                                     bpmn20XMLEntityId: UUID,
                                     processId: String) =
      ProcessInstanceCassandraEntity(id, bpmn20XMLEntityId, processId)

  override fun elementVisitEntity(id: UUID,
                                  bpmn20XMLEntityId: UUID,
                                  processId: String,
                                  processInstanceEntityId: UUID,
                                  flowElementId: String?,
                                  sourceRefId: String?,
                                  targetRefId: String?,
                                  fromFlowElementId: String?,
                                  time: Instant) =
      ElementVisitCassandraEntity(id, bpmn20XMLEntityId, processId, processInstanceEntityId,
          flowElementId, sourceRefId, targetRefId,
          when {
            // a SequenceFlow may have id set, but it isn't used for the key
            sourceRefId != null && targetRefId != null ->
              ElementVisitCassandraEntity.ElementType.SEQUENCE_FLOW
            flowElementId != null ->
              ElementVisitCassandraEntity.ElementType.FLOW_NODE
            else -> throw IllegalArgumentException(
                "If both sourceRefId and targetRefId are not provided," +
                    " flowElementId must be provided")
          },
          fromFlowElementId, time)
}
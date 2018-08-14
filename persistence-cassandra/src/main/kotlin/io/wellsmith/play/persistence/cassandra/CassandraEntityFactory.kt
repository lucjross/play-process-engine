package io.wellsmith.play.persistence.cassandra

import io.wellsmith.play.domain.ActivityStateChangeEntity
import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.persistence.cassandra.entity.ActivityStateChangeCassandraEntity
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
                                  fromFlowElementKey: String?,
                                  splitCorrelationId: UUID?,
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
          fromFlowElementKey, splitCorrelationId, time)

  override fun activityStateChangeEntity(id: UUID,
                                         processInstanceEntityId: UUID,
                                         activityId: String,
                                         fromFlowElementKey: String?,
                                         time: Instant,
                                         lifecycleId: UUID,
                                         state: ActivityStateChangeEntity.State,
                                         tokensArrived: Int,
                                         inputSetsNeedProcessing: Boolean,
                                         withdrawn: Boolean,
                                         workDone: Boolean,
                                         interruptedByError: Boolean,
                                         interruptedByNonError: Boolean,
                                         preCompletionStepsDone: Boolean,
                                         terminationStepsDone: Boolean) =
      ActivityStateChangeCassandraEntity(processInstanceEntityId, time, activityId, id,
          fromFlowElementKey, lifecycleId, state, tokensArrived, inputSetsNeedProcessing,
          withdrawn, workDone, interruptedByError, interruptedByNonError,
          preCompletionStepsDone, terminationStepsDone)
}
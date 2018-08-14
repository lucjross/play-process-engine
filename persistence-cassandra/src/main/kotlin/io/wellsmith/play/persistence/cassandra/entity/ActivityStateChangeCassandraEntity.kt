package io.wellsmith.play.persistence.cassandra.entity

import io.wellsmith.play.domain.ActivityStateChangeEntity
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("ActivityStateChange")
class ActivityStateChangeCassandraEntity(
    @PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    override val processInstanceEntityId: UUID,
    @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    override val time: Instant,
    @PrimaryKeyColumn(ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    override val activityId: String,
    override val id: UUID,
    override val fromFlowElementKey: String?,
    override val lifecycleId: UUID,
    override val state: ActivityStateChangeEntity.State,
    override val tokensArrived: Int,
    override val inputSetsNeedProcessing: Boolean,
    override val withdrawn: Boolean,
    override val workDone: Boolean,
    override val interruptedByError: Boolean,
    override val interruptedByNonError: Boolean,
    override val preCompletionStepsDone: Boolean,
    override val terminationStepsDone: Boolean
): ActivityStateChangeEntity {
}
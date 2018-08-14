package io.wellsmith.play.engine.activity

import io.wellsmith.play.domain.ActivityStateChangeEntity
import java.util.UUID

data class ActivityStateChange(val processInstanceEntityId: UUID,
                               val activityId: String,
                               val lifecycleId: UUID,
                               val priorState: ActivityStateChangeEntity.State?,
                               val newState: ActivityStateChangeEntity.State)
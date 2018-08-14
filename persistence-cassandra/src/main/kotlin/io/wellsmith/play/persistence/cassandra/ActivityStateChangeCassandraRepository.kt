package io.wellsmith.play.persistence.cassandra

import io.wellsmith.play.persistence.api.ActivityStateChangeRepository
import io.wellsmith.play.persistence.cassandra.entity.ActivityStateChangeCassandraEntity
import org.springframework.data.cassandra.repository.CassandraRepository
import java.util.UUID

interface ActivityStateChangeCassandraRepository:
    ActivityStateChangeRepository<ActivityStateChangeCassandraEntity>,
    CassandraRepository<ActivityStateChangeCassandraEntity, UUID> {

  override fun findByProcessInstanceEntityId(processInstanceEntityId: UUID):
      List<ActivityStateChangeCassandraEntity>
}
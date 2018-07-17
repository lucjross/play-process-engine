package io.wellsmith.playprocessengine.persistence.cassandra

import io.wellsmith.playprocessengine.persistence.api.EntityFactory
import io.wellsmith.playprocessengine.persistence.cassandra.entity.BPMN20XMLCassandraEntity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CassandraEntityFactory: EntityFactory {

  override fun bpmn20XMLEntity(id: UUID,
                               originalFilename: String,
                               document: String,
                               bundleId: UUID) =
      BPMN20XMLCassandraEntity(id, originalFilename, document, bundleId)
}
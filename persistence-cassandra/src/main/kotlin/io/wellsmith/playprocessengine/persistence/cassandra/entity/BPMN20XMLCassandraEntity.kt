package io.wellsmith.playprocessengine.persistence.cassandra.entity

import io.wellsmith.playprocessengine.domain.BPMN20XMLEntity
import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.mapping.Indexed
import org.springframework.data.cassandra.core.mapping.Table
import java.util.UUID

@Table("BPMN20XML")
data class BPMN20XMLCassandraEntity(
    @Id override val id: UUID,
    override val originalFilename: String,
    override val document: String,
    @Indexed override val bundleId: UUID): BPMN20XMLEntity {

  constructor(bpmn20XMLEntity: BPMN20XMLEntity): this(
      bpmn20XMLEntity.id,
      bpmn20XMLEntity.originalFilename,
      bpmn20XMLEntity.document,
      bpmn20XMLEntity.bundleId)
}
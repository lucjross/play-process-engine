package io.wellsmith.play.persistence.cassandra

import io.wellsmith.play.persistence.api.BPMN20XMLRepository
import io.wellsmith.play.persistence.cassandra.entity.BPMN20XMLCassandraEntity
import org.springframework.data.cassandra.repository.CassandraRepository
import java.util.UUID

interface BPMN20XMLCassandraRepository:
    BPMN20XMLRepository<BPMN20XMLCassandraEntity>,
    CassandraRepository<BPMN20XMLCassandraEntity, UUID> {

  override fun findByBundleId(bundleId: UUID): List<BPMN20XMLCassandraEntity>
}
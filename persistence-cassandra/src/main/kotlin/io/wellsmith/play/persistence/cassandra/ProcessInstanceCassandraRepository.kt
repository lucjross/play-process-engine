package io.wellsmith.play.persistence.cassandra

import io.wellsmith.play.persistence.api.ProcessInstanceRepository
import io.wellsmith.play.persistence.cassandra.entity.ProcessInstanceCassandraEntity
import org.springframework.data.cassandra.repository.CassandraRepository
import java.util.UUID

interface ProcessInstanceCassandraRepository:
    ProcessInstanceRepository<ProcessInstanceCassandraEntity>,
    CassandraRepository<ProcessInstanceCassandraEntity, UUID> {


}
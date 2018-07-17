package io.wellsmith.playprocessengine.persistence.cassandra

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean
import org.springframework.data.cassandra.config.SchemaAction
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories

@EnableCassandraRepositories
@Configuration
@ComponentScan
class PlayCassandraRepositoryConfiguration(
    @Value("\${spring.data.cassandra.keyspace-name}")
    private val keyspaceName: String,

    @Value("\${spring.data.cassandra.schema-action:CREATE_IF_NOT_EXISTS}")
    private val schemaAction: String,

    @Value("\${spring.data.cassandra.contact-points}")
    private val contactPoints: String,

    @Value("\${spring.data.cassandra.port}")
    private val port: Int
): AbstractCassandraConfiguration() {

  override fun cluster(): CassandraClusterFactoryBean = super.cluster()
      .apply {
        keyspaceCreations = listOf(
            CreateKeyspaceSpecification.createKeyspace(keyspaceName).ifNotExists())
      }

  override fun getKeyspaceName() = keyspaceName
  override fun getSchemaAction() = SchemaAction.valueOf(schemaAction.toUpperCase())
  override fun getContactPoints() = contactPoints
  override fun getPort() = port
}
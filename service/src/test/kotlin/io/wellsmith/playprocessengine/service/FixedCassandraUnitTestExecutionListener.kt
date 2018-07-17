package io.wellsmith.playprocessengine.service

import org.cassandraunit.spring.CassandraUnitDependencyInjectionIntegrationTestExecutionListener
import org.springframework.test.context.TestContext

class FixedCassandraUnitTestExecutionListener:
    CassandraUnitDependencyInjectionIntegrationTestExecutionListener() {

  override fun afterTestClass(testContext: TestContext) {
    try {
      super.afterTestClass(testContext)
    } catch (e: NullPointerException) {
      // a bug in cassandra-unit is causing a reference to a null Cluster field, so ignoring this.
    }
  }
}
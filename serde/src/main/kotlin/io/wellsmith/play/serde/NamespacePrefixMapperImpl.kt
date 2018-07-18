package io.wellsmith.play.serde

import com.sun.xml.bind.marshaller.NamespacePrefixMapper

class NamespacePrefixMapperImpl: NamespacePrefixMapper() {

  override fun getPreferredPrefix(
      namespaceUri: String?,
      suggestion: String?,
      requirePrefix: Boolean): String {

    return when(namespaceUri?.toLowerCase()) {
      null, "" ->
        ""
      "http://www.omg.org/spec/BPMN/20100524/MODEL".toLowerCase() ->
        "semantic"
      "http://www.omg.org/spec/DD/20100524/DC".toLowerCase() ->
        "dc"
      "http://www.omg.org/spec/BPMN/20100524/DI".toLowerCase() ->
        "bpmndi"
      "http://www.omg.org/spec/DD/20100524/DI".toLowerCase() ->
        "di"
      else ->
        throw UnsupportedOperationException("Unsupported namespace URI: $namespaceUri")
    }
  }
}
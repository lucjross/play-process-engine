package io.wellsmith.play.serde

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.omg.spec.bpmn._20100524.model.TProcess
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.diff.ComparisonListener
import org.xmlunit.diff.ComparisonType
import java.util.stream.Stream

class BPMN20SerdeTest {

  private val serde = BPMN20Serde()

  companion object {
    @JvmStatic
    fun files() = Stream.of(
        "/2010-06-03/Travel Booking/Tavel Booking.bpmn",
        "/2010-06-03/Pizza/triso - Order Process for Pizza V4.bpmn",
        "/2010-06-03/Order Fulfillment/Procurement Processes with Error Handling - Stencil Trisotech 3 pages.bpmn")
  }

  @ParameterizedTest
  @MethodSource("files")
  fun `back-and-forth unmarshalling & marshalling should work` (resource: String) {

    var inputStream = this::class.java.getResourceAsStream(
        resource)
    val definitions = serde.deserialize(inputStream)
    Assertions.assertTrue(
        definitions.rootElement.map { it.value }.count { it is TProcess } > 0)

    val baos = serde.serialize(definitions)
    val marshalledXML = baos.toString(Charsets.UTF_8.name())

    inputStream = this::class.java.getResourceAsStream(
        resource)
    DiffBuilder.compare(Input.from(inputStream))
        .withTest(marshalledXML)
        .withDifferenceListeners(ComparisonListener { comparison, outcome ->

          if (comparison.type !in listOf(
                  ComparisonType.XML_ENCODING)) {
            Assertions.fail<Unit>("Difference: $comparison")
          }
        })
        .build() // runs the diff
  }
}
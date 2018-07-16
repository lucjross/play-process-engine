package io.wellsmith.playprocessengine.serde

import org.omg.spec.bpmn._20100524.model.ObjectFactory
import org.omg.spec.bpmn._20100524.model.TDefinitions
import org.springframework.core.io.ClassPathResource
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.xml.bind.JAXBElement
import javax.xml.bind.Marshaller
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/**
 * Converts between XML and [TDefinitions].
 * "The Definitions class is the outermost containing object for all BPMN elements."
 * (§8.11, Definitions)
 */
class BPMN20Serde(private val marshaller: Jaxb2Marshaller): Serde<TDefinitions> {

  private val objectFactory = ObjectFactory()

  companion object {

    /**
     * Must be initialized before use by defining it as a bean
     * or by calling [Jaxb2Marshaller.afterPropertiesSet].
     */
    fun marshaller(): Jaxb2Marshaller {
      val marshaller = Jaxb2Marshaller()
      marshaller.setPackagesToScan(
          ObjectFactory::class.java.`package`.name,
          org.omg.spec.dd._20100524.dc.ObjectFactory::class.java.`package`.name,
          org.omg.spec.dd._20100524.di.ObjectFactory::class.java.`package`.name,
          org.omg.spec.bpmn._20100524.di.ObjectFactory::class.java.`package`.name)
      marshaller.setSchemas(
          ClassPathResource("spec/BPMN/20100501/BPMN20.xsd"),
          ClassPathResource("spec/BPMN/20100501/BPMNDI.xsd"),
          ClassPathResource("spec/BPMN/20100501/DC.xsd"),
          ClassPathResource("spec/BPMN/20100501/DI.xsd"),
          ClassPathResource("spec/BPMN/20100501/Semantic.xsd"))
      marshaller.setMarshallerProperties(mapOf(
          Marshaller.JAXB_FORMATTED_OUTPUT to true,
          "com.sun.xml.bind.namespacePrefixMapper" to NamespacePrefixMapperImpl()))
      return marshaller
    }
  }

  /**
   * @throws org.springframework.oxm.XmlMappingException
   */
  override fun serialize(obj: TDefinitions): ByteArrayOutputStream {
    val outputStream = ByteArrayOutputStream()
    val result = StreamResult(outputStream)
    marshaller.marshal(objectFactory.createDefinitions(obj), result)
    return outputStream
  }

  /**
   * @throws org.springframework.oxm.XmlMappingException
   */
  override fun deserialize(inputStream: InputStream): TDefinitions {
    val el = marshaller.unmarshal(StreamSource(inputStream))
    return (el as JAXBElement<*>).value as TDefinitions
  }
}
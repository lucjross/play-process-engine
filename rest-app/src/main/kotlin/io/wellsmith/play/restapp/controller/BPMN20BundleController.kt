package io.wellsmith.play.restapp.controller

import io.wellsmith.play.restapp.exception.BPMN20ValidationException
import io.wellsmith.play.service.BPMN20XMLService
import io.wellsmith.play.service.command.BPMN20XML
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.oxm.XmlMappingException
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping(path = ["bpmn20/bundle"])
class BPMN20BundleController(val bpmn20XMLService: BPMN20XMLService<*>) {

  companion object {
    private val logger = LoggerFactory.getLogger(BPMN20BundleController::class.java)
  }

  @PostMapping
  fun createBundle(@RequestParam("file") file: MultipartFile,
                   req: HttpServletRequest
  ): HttpEntity<Any?> {

    val bpmn20xmls = mutableListOf<BPMN20XML>()
    when (file.contentType) {
      "application/zip" -> {
        val zis = ZipInputStream(file.inputStream)
        var zipEntry: ZipEntry? = zis.nextEntry
        while (zipEntry != null) {
          if (!ignoreZipEntry(zipEntry)) {
            addToBundle(zis.readBytes(), zipEntry.name, bpmn20xmls)
          }

          zipEntry = zis.nextEntry
        }
      }
      "application/xml" -> {
        addToBundle(file.inputStream.readBytes(), file.originalFilename!!, bpmn20xmls)
      }
      else ->
        throw IllegalArgumentException("File content type ${file.contentType} not supported," +
            " must be application/xml or application/zip")
    }

    val bundleId: UUID
    try {
      bundleId = bpmn20XMLService.createBundle(bpmn20xmls)
    } catch (e: XmlMappingException) {
      throw BPMN20ValidationException(e)
    }

    val headers = HttpHeaders()
        .apply { add(HttpHeaders.LOCATION, "${req.requestURL}/$bundleId") }
    return ResponseEntity(null, headers, HttpStatus.CREATED)
  }

  @GetMapping(path = ["/{bundleId}"])
  fun getDefinitionsIdsInBundle(@PathVariable bundleId: UUID): HttpEntity<Collection<UUID>> {

    val ids = bpmn20XMLService.getDefinitionsIdsInBundle(bundleId)
    return ResponseEntity(ids, HttpStatus.OK)
  }

  private fun addToBundle(bytes: ByteArray,
                          originalFilename: String,
                          bundle: MutableCollection<BPMN20XML>) {

    // use the original XML to retain user's formatting
    val xml = bytes.toString(Charsets.UTF_8)
    bundle.add(BPMN20XML(originalFilename, xml, null))
  }
}

private fun ignoreZipEntry(zipEntry: ZipEntry): Boolean =
// ignore folders
    zipEntry.name.endsWith("/")
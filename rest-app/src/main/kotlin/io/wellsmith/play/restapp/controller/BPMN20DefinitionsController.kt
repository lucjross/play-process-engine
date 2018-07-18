package io.wellsmith.play.restapp.controller

import io.wellsmith.play.service.BPMN20XMLService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["bpmn20/definitions"])
class BPMN20DefinitionsController(val bpmn20XMLService: BPMN20XMLService<*>) {

  @GetMapping(path = ["/{id}"], produces = [MediaType.APPLICATION_XML_VALUE])
  fun getDefinitionsXML(@PathVariable id: UUID): String {

    return bpmn20XMLService.getDefinitionsXML(id)
  }
}
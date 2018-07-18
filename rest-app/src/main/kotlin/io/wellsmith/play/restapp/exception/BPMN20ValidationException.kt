package io.wellsmith.play.restapp.exception

import org.springframework.http.HttpStatus
import org.springframework.oxm.XmlMappingException
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BPMN20ValidationException(cause: XmlMappingException): RuntimeException(cause)
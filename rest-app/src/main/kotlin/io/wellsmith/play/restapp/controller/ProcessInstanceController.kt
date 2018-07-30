package io.wellsmith.play.restapp.controller

import io.wellsmith.play.service.ProcessInstanceService
import io.wellsmith.play.service.command.InstantiateProcess
import io.wellsmith.play.service.response.InstantiatedProcess
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["bpmn20/processInstance"])
class ProcessInstanceController(val processInstanceService: ProcessInstanceService) {

  @PostMapping
  fun instantiateProcess(@RequestBody instantiateProcess: InstantiateProcess):
      HttpEntity<InstantiatedProcess> {

    val instantiatedProcess = processInstanceService.instantiateProcess(
        instantiateProcess.bpmn20XMLEntityId, instantiateProcess.processId)
    return ResponseEntity(instantiatedProcess, HttpStatus.CREATED)
  }

  @GetMapping("/{id}/completed")
  fun isCompleted(@PathVariable("id") processInstanceEntityId: UUID): HttpEntity<Boolean> {

    val completed = processInstanceService.isCompleted(processInstanceEntityId)
    return ResponseEntity(completed, HttpStatus.OK)
  }
}
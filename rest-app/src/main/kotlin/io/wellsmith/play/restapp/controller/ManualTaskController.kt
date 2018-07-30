package io.wellsmith.play.restapp.controller

import io.wellsmith.play.service.ManualTaskService
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Deprecated("for demo")
@RestController
@RequestMapping(path = ["/bpmn20/manualTask"])
class ManualTaskController(private val manualTaskService: ManualTaskService) {

  @PostMapping(path = ["/{processInstanceEntityId}/{manualTaskId}/workIsDone"])
  fun workIsDone(@PathVariable("processInstanceEntityId") processInstanceEntityId: UUID,
                 @PathVariable("manualTaskId") manualTaskId: String
  ): HttpEntity<Any> {

    manualTaskService.workIsDone(processInstanceEntityId, manualTaskId)
    return ResponseEntity(HttpStatus.OK)
  }
}
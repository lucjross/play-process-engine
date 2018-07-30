package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TBaseElement
import java.util.concurrent.Future

abstract class BaseElementVisitor<T: TBaseElement>
internal constructor(val processInstance: ProcessInstance,
                     val el: T) {

  /**
   * Must check against [processInstance] whether the given element is currently visitable.
   */
  abstract fun visit(): List<Future<*>>
}
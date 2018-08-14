package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TBaseElement
import org.omg.spec.bpmn._20100524.model.TFlowElement
import java.util.UUID
import java.util.concurrent.Future

internal abstract class BaseElementVisitor<T: TBaseElement>
internal constructor(val processInstance: ProcessInstance,
                     val el: T) {

  /**
   * Implementations of this method must perform at least two sets of actions:
   *
   *   (a) all the operations that become necessary upon
   *       the traversal of a token to this [el] for this [processInstance].
   *
   *   (b) call [visit] upon other [BaseElementVisitor]s that are immediately
   *       visitable, during or following the execution of this method.
   *       The set of following elements that are "immediately visitable" as well
   *       as how and when they should be visited must be ascertained by the
   *       visiting visitor.
   *
   * The returned [Future]s allow for handling of exceptions, which primarily will consist of
   * those originating from persistence operations or any other actions done
   * asynchronously for performance reasons.
   *
   * The [fromFlowElement] parameter is required for most instances of [el],
   * namely those which have preceding sequence flows and are not visited automatically
   * upon process instantiation. An [IllegalArgumentException] is likely to arise
   * if the parameter is not provided when required.
   */
  internal abstract fun visit(fromFlowElement: TFlowElement?)
}
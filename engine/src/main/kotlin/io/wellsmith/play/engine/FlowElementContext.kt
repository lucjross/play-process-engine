package io.wellsmith.play.engine

import org.omg.spec.bpmn._20100524.model.TFlowElement

internal class FlowElementContext(val flowElement: TFlowElement,
                                  val relationshipToRoot: FlowElementNodeRelationship?)
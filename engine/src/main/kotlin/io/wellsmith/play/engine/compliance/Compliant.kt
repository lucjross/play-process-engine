package io.wellsmith.play.engine.compliance

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.EXPRESSION,
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Compliant(val toSpec: Spec,
                           val section: String = "N/A",
                           val table: String = "N/A",
                           val level: Level = Level.FULL,
                           val message: String = "")

enum class Spec {
  BPMN_2_0
}

enum class Level {
  INCOMPLETE, FULL, NON_COMPLIANT
}

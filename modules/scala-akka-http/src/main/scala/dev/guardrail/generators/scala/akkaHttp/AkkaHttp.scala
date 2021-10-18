package dev.guardrail.generators.scala.akkaHttp

import dev.guardrail.Target
import dev.guardrail.generators.scala.ScalaCollectionsGenerator
import dev.guardrail.generators.scala.ScalaGenerator
import dev.guardrail.generators.scala.ScalaLanguage
import dev.guardrail.generators.scala.CirceModelGenerator
import dev.guardrail.generators.scala.circe.CirceProtocolGenerator
import dev.guardrail.generators.{ Framework, SwaggerGenerator }

object AkkaHttp extends Framework[ScalaLanguage, Target] {
  implicit def CollectionsLibInterp = ScalaCollectionsGenerator()
  implicit def ClientInterp         = AkkaHttpClientGenerator(CirceModelGenerator.V012)
  implicit def FrameworkInterp      = AkkaHttpGenerator(CirceModelGenerator.V012)
  implicit def ProtocolInterp       = CirceProtocolGenerator(CirceModelGenerator.V012)
  implicit def ServerInterp         = AkkaHttpServerGenerator(CirceModelGenerator.V012)
  implicit def SwaggerInterp        = SwaggerGenerator[ScalaLanguage]
  implicit def LanguageInterp       = ScalaGenerator()
}

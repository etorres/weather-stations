package es.eriktorr.weather
package data.error

import cats.data.NonEmptyChain
import io.circe.syntax.EncoderOps

final case class ValidationErrors(errors: NonEmptyChain[String])
    extends ValidationError(errors.toNonEmptyList.toList.asJson.noSpaces)

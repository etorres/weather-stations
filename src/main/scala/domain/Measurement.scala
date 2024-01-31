package es.eriktorr.weather
package domain

import data.refined.Types.{Between, NonEmptyString}
import data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}
import domain.Measurement.{StationName, Temperature}

import cats.implicits.catsSyntaxValidatedIdBinCompat0
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

final case class Measurement(stationName: StationName, temperature: Temperature)

object Measurement:
  opaque type StationName <: String :| NonEmptyString = String :| NonEmptyString

  object StationName:
    def from(value: String): AllErrorsOr[StationName] = value.refineValidatedNec[NonEmptyString]

    def unsafeFrom(value: String): StationName = from(value).orFail

  opaque type Temperature <: Double :| Between[-99.0d, 99.0d] = Double :| Between[-99.0d, 99.0d]

  object Temperature:
    def from(value: String): AllErrorsOr[Temperature] = value.toDoubleOption match
      case Some(number) => from(number)
      case None => "Number expected".invalidNec

    def from(value: Double): AllErrorsOr[Temperature] =
      value.refineValidatedNec[Between[-99.0d, 99.0d]]

    def unsafeFrom(value: Double): Temperature = from(value).orFail
